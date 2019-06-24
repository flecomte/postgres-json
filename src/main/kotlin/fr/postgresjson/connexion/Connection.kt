package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import fr.postgresjson.entity.EntityI
import fr.postgresjson.serializer.Serializer
import java.util.concurrent.CompletableFuture

class Connection(
    private val database: String,
    private val username: String,
    private val password: String,
    private val host: String = "localhost",
    private val port: Int = 5432
) {
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

    fun <T, R : EntityI<T?>?> selectOne(sql: String, typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R? {
        val future = connect().sendPreparedStatement(sql, compileArgs(values))
        val json = future.get().rows[0].getString(0)
        return if (json === null) {
            null
        } else {
            serializer.deserialize(json, typeReference)
        }
    }

    inline fun <T, reified R : EntityI<T?>?> selectOne(sql: String, values: List<Any?> = emptyList()): R? = selectOne(sql, object: TypeReference<R>() {}, values)

    fun <T, R : List<EntityI<T?>?>> select(sql: String, typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R {
        val future = connect().sendPreparedStatement(sql, compileArgs(values))
        val json = future.get().rows[0].getString(0)
        return if (json === null) {
            listOf<EntityI<T?>?>() as R
        } else {
            serializer.deserializeList(json, typeReference)
        }
    }

    inline fun <T, reified R : List<EntityI<T?>?>> select(sql: String, values: List<Any?> = emptyList()): R = select(sql, object : TypeReference<R>() {}, values)

    fun exec(sql: String, values: List<Any?> = emptyList()): CompletableFuture<QueryResult> {
        return connect().sendPreparedStatement(sql, compileArgs(values))
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
}

