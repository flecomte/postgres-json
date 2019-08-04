package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.entity.EntityI
import fr.postgresjson.serializer.Serializer
import fr.postgresjson.utils.LoggerDelegate
import org.slf4j.Logger
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Connection as JDBCConnection

typealias SelectOneCallback<T> = ResultSet.(T?) -> Unit
typealias SelectCallback<T> = ResultSet.(List<T>) -> Unit
typealias SelectPaginatedCallback<T> = ResultSet.(Paginated<T>) -> Unit

class Connection(
    private val database: String,
    private val username: String,
    private val password: String,
    private val host: String = "localhost",
    private val port: Int = 5432
): Executable {
    private lateinit var connection: JDBCConnection
    private val serializer = Serializer()
    private val logger: Logger? by LoggerDelegate()

    internal fun connect(): JDBCConnection {
        if (!::connection.isInitialized || connection.isClosed) {
            connection = DriverManager.getConnection("jdbc:postgresql://$host:$port/$database", username, password)
        }
        return connection
    }

    fun <T> inTransaction(f: (Connection) -> T) {
        sendQuery("BEGIN")
        f(this)
        sendQuery("COMMIT")
    }

    override fun <R: EntityI<*>> select(
        sql: String,
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: (ResultSet, R?) -> Unit
    ): R? {
        val primaryObject = values.firstOrNull {
            it is EntityI<*> && typeReference.type.typeName == it::class.java.name
        } as R?
        val result = exec(sql, compileArgs(values))
        val json = result.getString(1)
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

    inline fun <reified R: EntityI<*>> selectOne(
        sql: String,
        values: List<Any?> = emptyList(),
        noinline block: SelectOneCallback<R> = {}
    ): R? =
        select(sql, object: TypeReference<R>() {}, values, block)

    override fun <R: EntityI<*>> select(
        sql: String,
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: (ResultSet, R?) -> Unit
    ): R? {
        return replaceArgs(sql, values) {
            select(this.sql, typeReference, this.parameters, block)
        }
    }

    inline fun <reified R: EntityI<*>> selectOne(
        sql: String,
        values: Map<String, Any?>,
        noinline block: SelectOneCallback<R> = {}
    ): R? =
        select(sql, object: TypeReference<R>() {}, values, block)

    override fun <R: EntityI<*>> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: List<Any?>,
        block: (ResultSet, List<R>) -> Unit
    ): List<R> {
        val result = exec(sql, compileArgs(values))
        val json = result.getString(1)
        return if (json === null) {
            listOf<EntityI<*>>() as List<R>
        } else {
            serializer.deserializeList(json, typeReference)
        }.also {
            block(result, it)
        }
    }

    inline fun <reified R: EntityI<*>> select(
        sql: String,
        values: List<Any?> = emptyList(),
        noinline block: SelectCallback<R> = {}
    ): List<R> =
        select(sql, object: TypeReference<List<R>>() {}, values, block)

    override fun <R: EntityI<*>> select(
        sql: String,
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: (ResultSet, Paginated<R>) -> Unit
    ): Paginated<R> {
        val offset = (page - 1) * limit
        val newValues = values
            .plus("offset" to offset)
            .plus("limit" to limit)

        val line = replaceArgs(sql, newValues) {
            exec(this.sql, this.parameters)
        }

        return line.run {
            val json = getString(1)
            val entities = if (json === null) {
                listOf<EntityI<*>>() as List<R>
            } else {
                serializer.deserializeList(json, typeReference)
            }
            Paginated(
                entities,
                offset,
                limit,
                getInt("total")
            )
        }.also {
            block(line, it)
        }
    }

    inline fun <reified R: EntityI<*>> select(
        sql: String,
        page: Int,
        limit: Int,
        values: Map<String, Any?> = emptyMap(),
        noinline block: SelectPaginatedCallback<R> = {}
    ): Paginated<R> =
        select(sql, page, limit, object: TypeReference<List<R>>() {}, values, block)

    override fun <R: EntityI<*>> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: (ResultSet, List<R>) -> Unit
    ): List<R> {
        return replaceArgs(sql, values) {
            select(this.sql, typeReference, this.parameters, block)
        }
    }

    inline fun <reified R: EntityI<*>> select(
        sql: String,
        values: Map<String, Any?>,
        noinline block: SelectCallback<R> = {}
    ): List<R> =
        select(sql, object: TypeReference<List<R>>() {}, values, block)

    override fun exec(sql: String, values: List<Any?>): ResultSet {
        return stopwatchQuery(sql, values) {
            connect().prepareStatement(sql).apply {
                compileArgs(values).forEachIndexed { i, v ->
                    when (v) {
                        is String  -> setString(i+1, v)
                        is Int  -> setInt(i+1, v)
                        else -> setString(i+1, v.toString())
                    }
                }
            }.executeQuery().apply { next() }
        }
    }

    override fun exec(sql: String, values: Map<String, Any?>): ResultSet {
        return replaceArgs(sql, values) {
            exec(this.sql, this.parameters)
        }
    }

    override fun sendQuery(sql: String, values: List<Any?>): Int {
        return stopwatchQuery(sql, values) {
            connect().prepareStatement(sql).apply {
                compileArgs(values).forEachIndexed { i, v ->
                    when (v) {
                        is String  -> setString(i+1, v)
                        is Int  -> setInt(i+1, v)
                        else -> setString(i+1, v.toString())
                    }
                }
            }.executeUpdate()
        }
    }

    private fun compileArgs(values: List<Any?>): List<Any?> {
        return values.map {
            if (it is EntityI<*>) {
                serializer.serialize(it).apply {
                    serializer.collection.set<Any?, EntityI<Any?>>(it as EntityI<Any?>)
                }
            } else {
                it
            }
        }
    }

    private fun <T> replaceArgs(sql: String, values: Map<String, Any?>, block: ParametersQuery.() -> T): T {
        val paramRegex = "(?<!:):([a-zA-Z0-9_-]+)".toRegex(RegexOption.IGNORE_CASE)
        val newArgs = paramRegex.findAll(sql).map { match ->
            val name = match.groups[1]!!.value
            values[name] ?: error("Parameter $name missing")
        }.toList()

        var newSql = sql
        values.forEach { (key, _) ->
            val regex = ":$key".toRegex()
            newSql = newSql.replace(regex, "?")
        }

        return block(ParametersQuery(newSql, newArgs))
    }

    data class ParametersQuery(val sql: String, val parameters: List<Any?>)

    private fun <T> stopwatchQuery(sql: String, values: List<Any?> = emptyList(), callback: () -> T): T {
        val sqlForLog = "\n${sql.prependIndent()}"
        try {
            val start = System.currentTimeMillis()
            val result = callback()
            val duration = (System.currentTimeMillis() - start)
            logger?.debug("$duration ms for query: $sqlForLog", values)
            return result
        } catch (e: Throwable) {
            logger?.info("Query Error: $sqlForLog, $values", e)
            throw e
        }

    }
}
