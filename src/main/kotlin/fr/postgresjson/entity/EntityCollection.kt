package fr.postgresjson.entity

class EntityCollection<T, E : EntityI<T>> {
    var collection: MutableMap<T, E> = mutableMapOf()

    fun get(id: T): E? {
        return collection[id]
    }

    fun set(entity: E) {
        collection.set(entity.id, entity)
    }
}
