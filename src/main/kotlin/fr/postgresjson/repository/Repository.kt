package fr.postgresjson.repository

import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import fr.postgresjson.Serializer
import fr.postgresjson.entity.EntityCollection
import fr.postgresjson.entity.EntityI

interface RepositoryI<T, E : EntityI<T>>

abstract class Repository<T, E : EntityI<T>> : RepositoryI<T, E> {
    abstract var connection: ConnectionPool<PostgreSQLConnection>

    fun <T> findById(id: T): EntityI<T?>? {
        return when (val e = EntityCollection().get(id)) {
            null -> {
                // TODO create Request
                Serializer().deserialize<T, EntityI<T?>>("""{"plop", "plip"}""")
            }
            else -> e
        }
    }
}