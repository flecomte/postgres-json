package fr.postgresjson.connexion

import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import fr.postgresjson.entity.EntityI
import fr.postgresjson.serializer.Serializer


class Connection(
    private val host: String = "localhost",
    private val port: Int = 5432,
    private val database: String = "dc-project",
    private val username: String = "dc-project",
    private val password: String = "dc-project"
) {
    private lateinit var connection: ConnectionPool<PostgreSQLConnection>
    val serializer = Serializer()

    fun connect(): ConnectionPool<PostgreSQLConnection> {
        if (!::connection.isInitialized || !connection.isConnected()) {
            connection = PostgreSQLConnectionBuilder.createConnectionPool(
                "jdbc:postgresql://$host:$port/$database?user=$username&password=$password"
            )
        }
        return connection
    }

    inline fun <T, reified R :EntityI<T?>> execute(sql: String, values: List<Any?> = emptyList()): R? {
        val future = connect().sendPreparedStatement(sql, values)
        val json = future.get().rows[0].getString(0)
        if (json === null) {
            return null
        } else {
            val obj = serializer.deserialize<T, R>(json)

            return obj
        }
    }
}