package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import fr.postgresjson.entity.EntityI
import fr.postgresjson.serializer.Serializer
import java.util.concurrent.CompletableFuture


interface Executable {
    fun <R : EntityI<*>> select(sql: String, typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R?
    fun <R : EntityI<*>> select(sql: String, typeReference: TypeReference<R>, values: Map<String, Any?>): R?
    fun <R : List<EntityI<*>>> select(sql: String, typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R?
    fun <R : List<EntityI<*>>> select(sql: String, typeReference: TypeReference<R>, values: Map<String, Any?>): R
    fun exec(sql: String, values: List<Any?> = emptyList()): CompletableFuture<QueryResult>
    fun exec(sql: String, values: Map<String, Any?>): CompletableFuture<QueryResult>
}

class Connection(
    private val database: String,
    private val username: String,
    private val password: String,
    private val host: String = "localhost",
    private val port: Int = 5432
): Executable {
    private lateinit var connection: ConnectionPool<PostgreSQLConnection>
    private val serializer = Serializer()

    fun connect(): ConnectionPool<PostgreSQLConnection> {
        if (!::connection.isInitialized || !connection.isConnected()) {
            connection = PostgreSQLConnectionBuilder.createConnectionPool(
                "jdbc:postgresql://$host:$port/$database?user=$username&password=$password"
            )
        }
        return connection
    }

    fun <A> inTransaction(f: (Connection) -> CompletableFuture<A>) = connect().inTransaction(f)

    override fun <R : EntityI<*>> select(sql: String, typeReference: TypeReference<R>, values: List<Any?>): R? {
        val future = connect().sendPreparedStatement(sql, compileArgs(values))
        val json = future.get().rows[0].getString(0)
        return if (json === null) {
            null
        } else {
            serializer.deserialize(json, typeReference)
        }
    }
    inline fun <reified R : EntityI<*>> selectOne(sql: String, values: List<Any?> = emptyList()): R? = select(sql, object: TypeReference<R>() {}, values)

    override fun <R : EntityI<*>> select(sql: String, typeReference: TypeReference<R>, values: Map<String, Any?>): R? {
        val replacedQuery = replaceArgs(sql, values)
        return select(replacedQuery.sql, typeReference, replacedQuery.parameters)
    }
    inline fun <reified R : EntityI<*>> selectOne(sql: String, values: Map<String, Any?>): R? = select(sql, object: TypeReference<R>() {}, values)

    override fun <R : List<EntityI<*>>> select(sql: String, typeReference: TypeReference<R>, values: List<Any?>): R {
        val future = connect().sendPreparedStatement(sql, compileArgs(values))
        val json = future.get().rows[0].getString(0)
        return if (json === null) {
            listOf<EntityI<*>>() as R
        } else {
            serializer.deserializeList(json, typeReference)
        }
    }
    inline fun <reified R : List<EntityI<*>>> select(sql: String, values: List<Any?> = emptyList()): R = select(sql, object : TypeReference<R>() {}, values)

    override fun <R : List<EntityI<*>>> select(sql: String, typeReference: TypeReference<R>, values: Map<String, Any?>): R {
        val replacedQuery = replaceArgs(sql, values)
        return select(replacedQuery.sql, typeReference, replacedQuery.parameters)
    }
    inline fun <reified R : List<EntityI<*>>> select(sql: String, values: Map<String, Any?>): R = select(sql, object : TypeReference<R>() {}, values)

    override fun exec(sql: String, values: List<Any?>): CompletableFuture<QueryResult> {
        return connect().sendPreparedStatement(sql, compileArgs(values))
    }

    override fun exec(sql: String, values: Map<String, Any?>): CompletableFuture<QueryResult> {
        val replacedQuery = replaceArgs(sql, values)
        return exec(replacedQuery.sql, replacedQuery.parameters)
    }

    private fun compileArgs(values: List<Any?>): List<Any?> {
        return values.map {
            if (it is EntityI<*>) {
                val json = serializer.serialize(it)
                serializer.collection.set<Any?, EntityI<Any?>>(it as EntityI<Any?>)
                json
            } else {
                it
            }
        }
    }

    private fun replaceArgs(sql: String, values: Map<String, Any?>): ParametersQuery {
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


        return ParametersQuery(newSql, newArgs)
    }

    data class ParametersQuery(val sql: String, val parameters: List<Any?>)
}

