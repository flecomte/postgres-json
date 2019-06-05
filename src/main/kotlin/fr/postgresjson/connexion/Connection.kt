package fr.postgresjson.connexion

import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import fr.postgresjson.entity.EntityI
import fr.postgresjson.serializer.Serializer
import java.io.File


class Connection(
    private val host: String = "localhost",
    private val port: Int = 5432,
    private val database: String = "dc-project",
    private val username: String = "dc-project",
    private val password: String = "dc-project",
    queriesDirectory: File? = null
) {
    private val queries = mutableMapOf<String, MutableMap<String, String>>()
    private lateinit var connection: ConnectionPool<PostgreSQLConnection>
    val serializer = Serializer()

    init {
        if (queriesDirectory === null) {
            val resource = this::class.java.getResource("/sql/query")
            if (resource !== null) {
                fetchQueries(File(resource.toURI()))
            }
        } else {
            fetchQueries(queriesDirectory)
        }
    }

    fun connect(): ConnectionPool<PostgreSQLConnection> {
        if (!::connection.isInitialized || !connection.isConnected()) {
            connection = PostgreSQLConnectionBuilder.createConnectionPool(
                "jdbc:postgresql://$host:$port/$database?user=$username&password=$password"
            )
        }
        return connection
    }

    inline fun <T, reified R : EntityI<T?>?> selectOne(group: String, name: String, values: List<Any?> = emptyList()): R? {
        val sql: String = getQuery(group, name)

        return selectOne<T, R>(sql, values)
    }

    inline fun <T, reified R : EntityI<T?>?> selectOne(sql: String, values: List<Any?> = emptyList()): R? {
        val future = connect().sendPreparedStatement(sql, values)
        val json = future.get().rows[0].getString(0)
        if (json === null) {
            return null
        } else {
            val obj = serializer.deserialize<T, R>(json)

            return obj
        }
    }

    inline fun <T, reified R : List<EntityI<T?>>> select(sql: String, values: List<Any?> = emptyList()): R {
        val future = connect().sendPreparedStatement(sql, values)
        val json = future.get().rows[0].getString(0)
        if (json === null) {
            return listOf<EntityI<T?>>() as R
        } else {
            val obj = serializer.deserializeList<R>(json)

            return obj
        }
    }

    private fun fetchQueries(queriesDirectory: File) {
        queriesDirectory.walk().filter{it.isDirectory}.forEach { directory ->
            val group = directory.name
            directory.walk().filter{it.isFile}.forEach { file ->
                val sql = file.readText()
                if (queries[group] === null) {
                    queries[group] = mutableMapOf()
                }
                queries[group]!![file.nameWithoutExtension] = sql
            }
        }
    }

    fun getQuery(group: String, name: String): String {
        if (queries[group] === null || queries[group]!![name] === null) {
            throw Exception("No query defined for $group/$name")
        }
        return queries[group]!![name]!!
    }
}