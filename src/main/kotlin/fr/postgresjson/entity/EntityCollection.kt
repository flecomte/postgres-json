package fr.postgresjson.entity

import kotlin.reflect.KClass

class EntityCollection {
    val collections: MutableMap<KClass<*>, EntityCollection<Any, EntityI<Any?>>> = mutableMapOf()

    inline fun <I, reified R : EntityI<I?>> get(id: I): R? {
        val collection = collections[R::class]
        val entity = collection?.get(id!!)
        return entity as R?
    }

    inline fun <I, reified R : EntityI<I?>> set(entity: R) {
        if (collections[R::class] == null) {
            collections[R::class] = EntityCollection()
        }

        collections[R::class]!!.set(entity as EntityI<Any?>)
    }

    class EntityCollection<T, E : EntityI<T?>> {
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
