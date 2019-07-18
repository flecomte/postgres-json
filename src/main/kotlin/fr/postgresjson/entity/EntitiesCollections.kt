package fr.postgresjson.entity

import kotlin.reflect.KClass

class EntitiesCollections {
    private val collections: MutableMap<KClass<*>, EntityCollection<Any, EntityI<Any?>>> = mutableMapOf()

    fun <I, R: EntityI<I?>> get(id: I, className: KClass<R>): R? {
        val collection = collections[className]
        val entity = collection?.get(id!!)
        return entity as R?
    }

    inline fun <I, reified R: EntityI<I?>> get(id: I): R? {
        return get(id, R::class)
    }

    fun <I, R: EntityI<out I?>> set(entity: R): EntitiesCollections {
        if (collections[entity.className] == null) {
            collections[entity.className] = EntityCollection()
        }

        collections[entity.className]!!.set(entity as EntityI<Any?>)

        return this
    }

    class EntityCollection<T, E: EntityI<T?>> {
        private var collection: MutableMap<T, E> = mutableMapOf()

        fun get(id: T): E? {
            return collection[id]
        }

        fun set(entity: E) {
            val id = entity.id
            if (id !== null) {
                collection[id] = entity
            }
        }
    }
}
