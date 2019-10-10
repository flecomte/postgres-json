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
import fr.postgresjson.entity.EntityI

class Serializer(val mapper: ObjectMapper = jacksonObjectMapper()) {
    init {
        val module = SimpleModule()
        mapper.registerModule(module)
        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

        mapper.registerModule(JodaModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun serialize(source: EntityI, pretty: Boolean = false): String {
        return if (pretty) mapper.writerWithDefaultPrettyPrinter().writeValueAsString(source)
        else mapper.writeValueAsString(source)
    }

    fun <E : EntityI> deserialize(json: String, valueTypeRef: TypeReference<E>): E {
        return this.mapper.readValue(json, valueTypeRef)
    }

    inline fun <reified E : EntityI> deserialize(json: String): E? {
        return this.mapper.readValue(json)
    }

    fun <E> deserializeList(json: String, valueTypeRef: TypeReference<E>): E {
        return mapper.readValue(json, valueTypeRef)
    }

    inline fun <reified E> deserializeList(json: String): E {
        return deserializeList(json, object : TypeReference<E>() {})
    }

    fun <E : EntityI> deserialize(json: String, target: E): E {
        return mapper.readerForUpdating(target).readValue<E>(json)
    }
}

fun EntityI.serialize(pretty: Boolean = false) = Serializer().serialize(this, pretty)
inline fun <reified E : EntityI> E.deserialize(json: String) = Serializer().deserialize(json, this)
inline fun <reified E : EntityI> String.deserialize() = Serializer().deserialize<E>(this)