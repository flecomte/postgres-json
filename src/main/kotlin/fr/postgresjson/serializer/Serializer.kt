package fr.postgresjson.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.postgresjson.entity.EntityCollection
import fr.postgresjson.entity.EntityI

class Serializer(val mapper: ObjectMapper = jacksonObjectMapper()) {
    fun <T> serialize(source: EntityI<T>): String {
        return mapper.writeValueAsString(source)
    }

    inline fun <T, reified E : EntityI<T>> deserialize(json: String): E {
        val unserialized = mapper.readValue<E>(json)
        return EntityCollection<T, E>().get(unserialized.id) ?: unserialized
    }

    fun <T, E : EntityI<T>> deserialize(json: String, target: E): E {
        val unserialized = mapper.readerForUpdating(target).readValue<E>(json)
        return EntityCollection<T, E>().get(unserialized.id) ?: unserialized
    }
}

fun <T> EntityI<T>.serialize() = Serializer().serialize(this)
fun <T, E : EntityI<T>> E.deserialize(json: String) = Serializer().deserialize(json, this)