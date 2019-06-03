package fr.postgresjson.repository

import fr.postgresjson.entity.EntityCollection
import fr.postgresjson.entity.EntityI
import fr.postgresjson.serializer.Serializer
import kotlin.reflect.KClass

interface RepositoryI<T, E : EntityI<T>>

abstract class Repository<T, E : EntityI<T>> : RepositoryI<T, E> {
    private val collections: MutableMap<KClass<*>, EntityCollection<Any, EntityI<Any>>> = mutableMapOf()

    private inline fun <I, reified R : EntityI<I>> get(id: I): R? {
        val collection = collections[R::class]
        val entity = collection?.get(id!!)
        return entity as R?
    }

    private inline fun <I, reified R : EntityI<I>> set(entity: R) {
        if (collections[R::class] == null) {
            collections[R::class] = EntityCollection()
        }

        collections[R::class]!!.set(entity as EntityI<Any>)
    }

    fun <T> findById(id: T): EntityI<T>? {
        return when (val e = get(id)) {
            null -> {
                // TODO create Request
                Serializer().deserialize<T, EntityI<T>>("""{"plop", "plip"}""")
            }
            else -> e

        }
    }
}