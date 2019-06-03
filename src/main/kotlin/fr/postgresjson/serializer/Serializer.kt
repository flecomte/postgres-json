package fr.postgresjson.serializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.postgresjson.entity.EntityCollection
import fr.postgresjson.entity.EntityI
import fr.postgresjson.entity.IdEntity
import fr.postgresjson.entity.UuidEntity
import java.io.IOException
import java.util.*


class Serializer(val mapper: ObjectMapper = jacksonObjectMapper()) {

    var collection: EntityCollection = EntityCollection()

    init {
        val module = SimpleModule()
        module.addDeserializer(UuidEntity::class.java, EntityUuidDeserializer(collection))
        module.addDeserializer(IdEntity::class.java, EntityIdDeserializer(collection))
        mapper.registerModule(module)
    }

    fun <T> serialize(source: EntityI<T>): String {
        return mapper.writeValueAsString(source)
    }

    inline fun <T, reified E : EntityI<T?>> deserialize(json: String): E {
        val unserialized = this.mapper.readValue<E>(json)
        return collection.get(unserialized.id) ?: unserialized
    }

    inline fun <T, reified E : EntityI<T?>> deserialize(json: String, target: E): E {
        val unserialized = mapper.readerForUpdating(target).readValue<E>(json)
        return collection.get(unserialized.id) ?: unserialized
    }
}

fun <T> EntityI<T?>.serialize() = Serializer().serialize(this)
inline fun <T, reified E : EntityI<T?>> E.deserialize(json: String) = Serializer().deserialize(json, this)


class EntityUuidDeserializer <T: UuidEntity> @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<T>(vc) {
    var collection: EntityCollection  = EntityCollection()

    constructor(collection: EntityCollection) : this() {
        this.collection = collection
    }

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): T {
        val node = jp.codec.readTree<JsonNode>(jp)
        val id = node.get("id").asText()
        val entity = collection.get<UUID, UuidEntity>(UUID.fromString(id))

        return (entity ?: ctxt.readValue(jp, UuidEntity::class.javaObjectType)) as T
    }
}


class EntityIdDeserializer <T: IdEntity> @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<T>(vc) {
    var collection: EntityCollection  = EntityCollection()

    constructor(collection: EntityCollection) : this() {
        this.collection = collection
    }

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): T {
        val node = jp.codec.readTree<JsonNode>(jp)
        val id = node.get("id").asInt()
        val entity = collection.get<Int?, IdEntity>(id)

        val obj = (entity ?: ctxt.readValue(jp, UuidEntity::class.javaObjectType)) as EntityI<Int?>
        collection.set(obj)

        return obj as T
    }
}