package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import com.github.jasync.sql.db.util.length
import fr.postgresjson.entity.EntityI
import fr.postgresjson.entity.Serializable
import fr.postgresjson.serializer.Serializer
import fr.postgresjson.utils.LoggerDelegate
import org.slf4j.Logger
import java.util.concurrent.CompletableFuture

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
        connection?.run { disconnect() }
    }

    fun <A> inTransaction(f: (Connection) -> CompletableFuture<A>) = connect().inTransaction(f)

    override fun <R : EntityI> select(
        sql: String,
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? {
        val primaryObject = values.firstOrNull {
            it is EntityI && typeReference.type.typeName == it::class.java.name
        } as R?
        val result = exec(sql, compileArgs(values))
        val json = result.rows.firstOrNull()?.getString(0)
        return if (json === null) {
            null
        } else {
            if (primaryObject != null) {
                serializer.deserialize(json, primaryObject)
            } else {
                serializer.deserialize(json, typeReference)
            }
        }.also {
            block(result, it)
        }
    }

    inline fun <reified R : EntityI> selectOne(
        sql: String,
        values: List<Any?> = emptyList(),
        noinline block: SelectOneCallback<R> = {}
    ): R? =
        select(sql, object : TypeReference<R>() {}, values, block)

    override fun <R : EntityI> select(
        sql: String,
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? {
        return replaceArgs(sql, values) {
            select(this.sql, typeReference, this.parameters, block)
        }
    }

    inline fun <reified R : EntityI> selectOne(
        sql: String,
        values: Map<String, Any?>,
        noinline block: SelectOneCallback<R> = {}
    ): R? =
        select(sql, object : TypeReference<R>() {}, values, block)

    override fun <R : EntityI> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: List<Any?>,
        block: (QueryResult, List<R>) -> Unit
    ): List<R> {
        val result = exec(sql, compileArgs(values))
        val json = result.rows[0].getString(0)
        return if (json === null) {
            listOf<EntityI>() as List<R>
        } else {
            serializer.deserializeList(json, typeReference)
        }.also {
            block(result, it)
        }
    }

    inline fun <reified R : EntityI> select(
        sql: String,
        values: List<Any?> = emptyList(),
        noinline block: SelectCallback<R> = {}
    ): List<R> =
        select(sql, object : TypeReference<List<R>>() {}, values, block)

    override fun <R : EntityI> select(
        sql: String,
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: (QueryResult, Paginated<R>) -> Unit
    ): Paginated<R> {
        val offset = (page - 1) * limit
        val newValues = values
            .plus("offset" to offset)
            .plus("limit" to limit)

        val line = replaceArgs(sql, newValues) {
            exec(this.sql, this.parameters)
        }

        return line.run {
            val json = rows[0].getString(0)
            val entities = if (json === null) {
                listOf<EntityI>() as List<R>
            } else {
                serializer.deserializeList(json, typeReference)
            }
            Paginated(
                entities,
                offset,
                limit,
                rows[0].getInt("total") ?: error("The query not return total")
            )
        }.also {
            block(line, it)
        }
    }

    inline fun <reified R : EntityI> select(
        sql: String,
        page: Int,
        limit: Int,
        values: Map<String, Any?> = emptyMap(),
        noinline block: SelectPaginatedCallback<R> = {}
    ): Paginated<R> =
        select(sql, page, limit, object : TypeReference<List<R>>() {}, values, block)

    override fun <R : EntityI> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: (QueryResult, List<R>) -> Unit
    ): List<R> {
        return replaceArgs(sql, values) {
            select(this.sql, typeReference, this.parameters, block)
        }
    }

    inline fun <reified R : EntityI> select(
        sql: String,
        values: Map<String, Any?>,
        noinline block: SelectCallback<R> = {}
    ): List<R> =
        select(sql, object : TypeReference<List<R>>() {}, values, block)

    override fun exec(sql: String, values: List<Any?>): QueryResult {
        val compiledValues = compileArgs(values)
        return stopwatchQuery(sql, compiledValues) {
            connect().sendPreparedStatement(sql, compiledValues).join()
        }
    }

    override fun exec(sql: String, values: Map<String, Any?>): QueryResult {
        return replaceArgs(sql, values) {
            exec(this.sql, this.parameters)
        }
    }

    override fun sendQuery(sql: String, values: List<Any?>): Int {
        val compiledValues = compileArgs(values)
        return stopwatchQuery(sql, compiledValues) {
            replaceArgsIntoSql(sql, compiledValues) {
                connect().sendQuery(it).join().rowsAffected.toInt()
            }
        }
    }

    override fun sendQuery(sql: String, values: Map<String, Any?>): Int {
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
        val paramRegex = "(?<!:):([a-zA-Z0-9_-]+)".toRegex(RegexOption.IGNORE_CASE)
        val newArgs = paramRegex.findAll(sql).map { match ->
            val name = match.groups[1]!!.value
            values[name] ?: values[name.trimStart('_')] ?: error("Parameter $name missing")
        }.toList()

        var newSql = sql
        values.forEach { (key, _) ->
            val regex = ":_?$key".toRegex()
            newSql = newSql.replace(regex, "?")
        }

        return block(ParametersQuery(newSql, newArgs))
    }

    private fun <T> replaceArgsIntoSql(sql: String, values: List<Any?>, block: (String) -> T): T {
        val paramRegex = "(?<!\\?)(\\?)(?!\\?)".toRegex(RegexOption.IGNORE_CASE)
        var i = 0
        if (values.isNotEmpty()) {
            val newSql = paramRegex.replace(sql) {
                values[i] ?: error("Parameter $i missing")
                val valToReplace = values[i].toString()
                ++i
                "'$valToReplace'"
            }

            return block(newSql)
        }

        return block(sql)
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
}
