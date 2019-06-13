package fr.postgresjson.repository

import fr.postgresjson.Serializer
import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.EntityCollection
import fr.postgresjson.entity.EntityI
import kotlin.reflect.KClass

interface RepositoryI<T, E : EntityI<T?>> {
    val entityName: KClass<E>
}

abstract class Repository<T, E : EntityI<T?>>(override val entityName: KClass<E>) : RepositoryI<T, E> {

    abstract var connection: Connection
    abstract fun getClassName(): String

    fun <T> findById(id: T): EntityI<T?>? {
        val sql = this.connection.getQuery(entityName.toString())
        return when (val e = EntityCollection().get(id)) {
            null -> {
                // TODO create Request
                Serializer().deserialize<T, EntityI<T?>>("""{"plop", "plip"}""")
            }
            else -> e
        }
    }
}
