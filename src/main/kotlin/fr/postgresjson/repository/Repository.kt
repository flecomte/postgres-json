package fr.postgresjson.repository

import fr.postgresjson.Serializer
import fr.postgresjson.connexion.Requester
import fr.postgresjson.entity.EntitiesCollections
import fr.postgresjson.entity.EntityI
import kotlin.reflect.KClass

interface RepositoryI<T, E : EntityI<T?>> {
    val entityName: KClass<E>
}

abstract class Repository<T, E : EntityI<T?>>(override val entityName: KClass<E>) : RepositoryI<T, E> {

    abstract var requester: Requester
    abstract fun getClassName(): String

    fun <T> findById(id: T): EntityI<T?>? {
        val sql = requester.getQuery(entityName.toString())
        return when (val e = EntitiesCollections().get(id)) {
            null -> {
                // TODO create Request
                Serializer().deserialize<T, EntityI<T?>>("""{"plop", "plip"}""")
            }
            else -> e
        }
    }
}
