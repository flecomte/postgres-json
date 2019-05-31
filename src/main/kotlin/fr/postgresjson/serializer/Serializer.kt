package fr.postgresjson.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.postgresjson.entity.Entity

class Serializer(val mapper: ObjectMapper = jacksonObjectMapper()) {
    fun serialize(source: Any): String {
        return mapper.writeValueAsString(source)
    }

    inline fun <reified T>deserialize(json: String): T {
        return mapper.readValue(json)
    }

    inline fun <reified T>deserialize(json: String, target: T): T {
        return mapper.readerForUpdating(target).readValue(json)
    }
}

fun <T> Entity<T>.serialize() = Serializer().serialize(this)
inline fun <reified T> T.deserialize(json: String) = Serializer().deserialize(json, this)