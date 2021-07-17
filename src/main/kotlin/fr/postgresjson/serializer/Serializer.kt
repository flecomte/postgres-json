package fr.postgresjson.serializer

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.postgresjson.entity.Serializable

class Serializer(val mapper: ObjectMapper = jacksonObjectMapper()) {
    init {
        val module = SimpleModule()
        mapper.registerModule(module)
        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

        mapper.registerModule(JodaModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun serialize(source: Any, pretty: Boolean = false): String {
        return if (pretty) mapper.writerWithDefaultPrettyPrinter().writeValueAsString(source)
        else mapper.writeValueAsString(source)
    }

    fun <E> deserialize(json: String, valueTypeRef: TypeReference<E>): E {
        return this.mapper.readValue(json, valueTypeRef)
    }

    inline fun <reified E> deserialize(json: String): E? {
        return this.mapper.readValue(json)
    }

    fun <E> deserializeList(json: String, valueTypeRef: TypeReference<E>): E {
        return mapper.readValue(json, valueTypeRef)
    }

    inline fun <reified E> deserializeList(json: String): E {
        return deserializeList(json, object : TypeReference<E>() {})
    }
}

fun Serializable.serialize(pretty: Boolean = false) = Serializer().serialize(this, pretty)
fun List<Serializable>.serialize(pretty: Boolean = false) = Serializer().serialize(this, pretty)
inline fun <reified E : Serializable> String.deserialize() = Serializer().deserialize<E>(this)

inline fun <reified T : Serializable> T.toTypeReference(): TypeReference<T> = object : TypeReference<T>() {}
