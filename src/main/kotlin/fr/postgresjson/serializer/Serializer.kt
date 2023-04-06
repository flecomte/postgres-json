package fr.postgresjson.serializer

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.jasync.sql.db.QueryResult

class Serializer(val mapper: ObjectMapper = jacksonObjectMapper()) {
    init {
        val module = SimpleModule()
        mapper.registerModule(module)
        mapper.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE

        mapper.registerModule(JodaModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun serialize(source: Any, pretty: Boolean = false): String {
        return if (pretty) mapper.writerWithDefaultPrettyPrinter().writeValueAsString(source)
        else mapper.writeValueAsString(source)
    }

    fun <E> deserialize(json: String, valueTypeRef: TypeReference<E>): E {
        return mapper.readValue(json, valueTypeRef)
    }

    inline fun <reified E> deserialize(json: String): E? {
        return this.mapper.readValue(json)
    }
}

inline fun <reified E : Any?> QueryResult.deserialize(): E? {
    val value = this.rows.firstOrNull()?.getString(0)
    return if (value == null) {
        null
    } else {
        Serializer().deserialize<E>(value)
    }
}

inline fun <reified T : Any> T.toTypeReference(): TypeReference<T> = object : TypeReference<T>() {}
