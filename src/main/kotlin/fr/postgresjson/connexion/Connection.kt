package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.general.ArrayRowData
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import com.github.jasync.sql.db.util.length
import fr.postgresjson.entity.EntityI
import fr.postgresjson.entity.Serializable
import fr.postgresjson.serializer.Serializer
import fr.postgresjson.utils.LoggerDelegate
import org.slf4j.Logger
import kotlin.random.Random

typealias SelectOneCallback<T> = QueryResult.(T?) -> Unit
typealias SelectCallback<T> = QueryResult.(List<T>) -> Unit
typealias SelectPaginatedCallback<T> = QueryResult.(Paginated<T>) -> Unit

class Connection(
    private val database: String,
    private val username: String,
    private val password: String,
    private val host: String = "localhost",
    private val port: Int = 5432
) : Executable {
    private var connection: ConnectionPool<PostgreSQLConnection>? = null
    private val serializer = Serializer()
    private val logger: Logger? by LoggerDelegate()

    internal fun connect(): ConnectionPool<PostgreSQLConnection> {
        return connection.let { connectionPool ->
            if (connectionPool == null || !connectionPool.isConnected()) {
                PostgreSQLConnectionBuilder.createConnectionPool(
                    "jdbc:postgresql://$host:$port/$database?user=$username&password=$password"
                ).also {
                    connection = it
                }
            } else {
                connectionPool
            }
        }
    }

    fun disconnect() {
        connection?.disconnect()
    }

    fun <A> inTransaction(block: Connection.() -> A?): A? = connect().run {
        sendQuery("BEGIN")
        try {
            block().apply { sendQuery("COMMIT") }
        } catch (e: Throwable) {
            sendQuery("ROLLBACK")
            throw e
        }
    }

    /**
     * Select One [EntityI] with [List] of parameters
     */
    override fun <R : EntityI> selectOne(
        sql: String,
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? {
        val result = exec(sql, compileArgs(values))
        val json = result.rows.firstOrNull()?.getString(0)
        return if (json === null) {
            null
        } else {
            serializer.deserialize(json, typeReference)
        }.also {
            block(result, it)
        }
    }

    /**
     * Select One [EntityI] with named parameters
     */
    override fun <R : EntityI> selectOne(
        sql: String,
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? {
        return replaceArgs(sql, values) {
            selectOne(this.sql, typeReference, parameters, block)
        }
    }

    /* Select Multiples */

    /**
     * Select multiple [EntityI] with [List] of parameters
     */
    override fun <R : EntityI> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: List<Any?>,
        block: QueryResult.(List<R>) -> Unit
    ): List<R> {
        val result = exec(sql, values)
        val json = result.rows[0].getString(0)
        return if (json === null) {
            emptyList()
        } else {
            serializer.deserializeList(json, typeReference)
        }.also {
            block(result, it)
        }
    }

    /**
     * Select multiple [EntityI] with [Map] of parameters
     */
    override fun <R : EntityI> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: QueryResult.(List<R>) -> Unit
    ): List<R> {
        return replaceArgs(sql, values) {
            select(this.sql, typeReference, this.parameters, block)
        }
    }

    /* Select Paginated */

    /**
     * Select Multiple [EntityI] with pagination
     */
    override fun <R : EntityI> select(
        sql: String,
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: QueryResult.(Paginated<R>) -> Unit
    ): Paginated<R> {
        val offset = (page - 1) * limit
        val newValues = values
            .plus("offset" to offset)
            .plus("limit" to limit)

        val line = replaceArgs(sql, newValues) {
            exec(this.sql, this.parameters)
        }

        return line.run {
            val firstLine = rows.firstOrNull() ?: queryError("The query has no return", sql, newValues)
            if (!(firstLine as ArrayRowData).mapping.keys.contains("total")) queryError("""The query not return the "total" column""", sql, newValues, rows)
            val total = try {
                firstLine.getInt("total") ?: queryError("The query return \"total\" must not be null", sql, newValues, rows)
            } catch (e: ClassCastException) {
                queryError("""Column "total" must be an integer""", sql, newValues, rows)
            }
            val json = firstLine.getString(0)
            val entities = if (json == null) {
                emptyList()
            } else {
                serializer.deserializeList(json, typeReference)
            }
            Paginated(
                entities,
                offset,
                limit,
                total
            )
        }.also {
            block(line, it)
        }
    }

    override fun exec(sql: String, values: List<Any?>): QueryResult {
        val compiledValues = compileArgs(values)
        return stopwatchQuery(sql, compiledValues) {
            connect().sendPreparedStatement(replaceNamedArgByQuestionMark(sql), compiledValues).join()
        }
    }

    override fun exec(sql: String, values: Map<String, Any?>): QueryResult {
        return replaceArgs(sql, values) {
            exec(this.sql, this.parameters)
        }
    }

    /**
     * Warning: this method not use prepared statement
     */
    override fun sendQuery(sql: String, values: List<Any?>): QueryResult {
        val compiledValues = compileArgs(values)
        return stopwatchQuery(sql, compiledValues) {
            replaceArgsIntoSql(sql, compiledValues) {
                connect().sendQuery(it).join()
            }
        }
    }

    /**
     * Warning: this method not use prepared statement
     */
    override fun sendQuery(sql: String, values: Map<String, Any?>): QueryResult {
        return replaceArgs(sql, values) {
            sendQuery(this.sql, this.parameters)
        }
    }

    private fun compileArgs(values: List<Any?>): List<Any?> {
        return values.map {
            if (it is Serializable || (it is List<*> && it.firstOrNull() is Serializable)) {
                serializer.serialize(it)
            } else {
                it
            }
        }
    }

    private fun <T> replaceArgs(sql: String, values: Map<String, Any?>, block: ParametersQuery.() -> T): T {
        val paramRegex = "(?<!:):([a-z0-9_-]+)".toRegex(RegexOption.IGNORE_CASE)
        val orderedArgs = paramRegex.findAll(sql).map { match ->
            val name = match.groups[1]!!.value
            values[name] ?: values[name.trimStart('_')] ?: queryError("""Parameter "$name" missing""", sql, values)
        }.toList()

        return block(ParametersQuery(replaceNamedArgByQuestionMark(sql), orderedArgs))
    }

    private fun replaceNamedArgByQuestionMark(sql: String): String =
        "(?<!:):([a-z0-9_-]+)"
            .toRegex(RegexOption.IGNORE_CASE)
            .replace(sql, "?")

    private fun insertArgsValuesIntoSql(sql: String, values: List<Any?>): String {
        var i = 0

        /* The regular expression matches a question mark "?" alone, not preceded or followed by another question mark */
        return """(?<!\?)(\?)(?!\?)"""
            .toRegex(RegexOption.IGNORE_CASE)
            .replace(sql) {
                values.getOrNull(i)
                    ?.toString()
                    ?.also { ++i }
                    ?.let(this::escapeParameter)
                    ?: queryError("Parameter $i missing", sql, values)
            }
    }

    private fun <T> replaceArgsIntoSql(sql: String, values: List<Any?>, block: (String) -> T): T {
        return if (values.isNotEmpty()) {
            sql
                .let(this::replaceNamedArgByQuestionMark)
                .let { insertArgsValuesIntoSql(it, values) }
                .let(block)
        } else block(sql)
    }

    /**
     * Escape parameter by generate a random tag to prevent SQL injection
     */
    private fun escapeParameter(parameter: String): String {
        val escapeTag = escapeTag().let {
            if (parameter.indexOf(it) >= 0) escapeParameter(parameter) else it
        }
        return """$escapeTag$parameter$escapeTag"""
    }

    /**
     * Generate a random alphaNum tag of 8 characters
     */
    private fun escapeTag(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z')
        val tagName = (1..8)
            .map { _ -> Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
        return "\$$tagName\$"
    }

    data class ParametersQuery(val sql: String, val parameters: List<Any?>)

    private fun <T> stopwatchQuery(sql: String, values: List<Any?> = emptyList(), callback: () -> T): T {
        try {
            val start = System.currentTimeMillis()
            val result = callback()
            val duration = (System.currentTimeMillis() - start)
            val resultText = when (result) {
                null -> "with no result"
                is QueryResult -> result.rows.firstOrNull()?.joinToString(", ")?.let { text ->
                    if (text.length > 100) "${text.take(100)}... (size: ${text.length})" else text
                } ?: "with no result"
                else -> "unknown"
            }
            val args = """
                |Query ($duration ms):
                |${sql.trimIndent().prependIndent()}
                |Arguments (${values.length}):
                |${values.joinToString("\n").ifBlank { "No arguments" }.prependIndent()}
                |Result:
                |${resultText.trimIndent().prependIndent()}
            """.trimMargin().prependIndent(" > ")
            logger?.debug("Query executed in $duration ms \n{}", args)
            return result
        } catch (e: Throwable) {
            logger?.info(
                """
                Query Error: 
                ${sql.prependIndent()}, 
                ${values.joinToString(", ").prependIndent()}
                """.trimIndent(),
                e
            )
            throw e
        }
    }

    class QueryError(msg: String) : Exception(msg)

    private fun queryError(
        msg: String,
        sql: String,
        parameters: List<Any?>,
        result: ResultSet? = null
    ): Nothing = throw QueryError(
        """
        |$msg
        |
        |${parameters.joinToString(", ") { it.toString() }.prependIndent("  > ")}
        |${sql.prependIndent("  > ")}
        |${result?.let { "-----" }?.prependIndent("  > ") ?: ""}
        |${result?.columnNames()?.joinToString(" | ")?.prependIndent("  > ") ?: ""}
        |${result?.map { it.joinToString(" | ") }?.joinToString("\n")?.prependIndent("  > ") ?: ""}
        """.trimMargin().trim(' ', '\n')
    )

    private fun queryError(
        msg: String,
        sql: String,
        parameters: Map<String, Any?>,
        result: ResultSet? = null
    ): Nothing = throw QueryError(
        """
        |$msg
        |
        |${parameters.map { ":" + it.key + " = " + it.value }.joinToString(", ").prependIndent("  > ")}
        |${sql.prependIndent("  > ")}
        |${result?.let { "-----" }?.prependIndent("  > ") ?: ""}
        |${result?.columnNames()?.joinToString(" | ")?.prependIndent("  > ") ?: ""}
        |${result?.map { it.joinToString(" | ") }?.joinToString("\n")?.prependIndent("  > ") ?: ""}
        """.trimMargin().trim(' ', '\n')
    )
}
