package fr.postgresjson

import fr.postgresjson.entity.mutable.IdEntity
import fr.postgresjson.serializer.Serializer
import fr.postgresjson.serializer.deserialize
import fr.postgresjson.serializer.serialize
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SerializerTest {
    private class ObjTest(var val1: String, var val2: Int) : IdEntity(1)
    private class ObjTestDate(var val1: DateTime) : IdEntity(2)

    private val serializer = Serializer()

    private val objSerialized: String = """{"val1":"plop","val2":123,"id":2}"""
    private val objSerializedWithExtra: String = """{"val1":"plop","val2":123,"id":2,"toto":"tata"}"""
    private val objSerializedUpdate = """{"val1":"update","val2":123}"""
    private lateinit var obj: ObjTest

    @BeforeEach
    fun before() {
        obj = ObjTest("plop", 123)
        obj.id = 2
    }

    @Test
    fun serialize() {
        val json = serializer.serialize(obj)
        assertTrue(json.contains(""""val1":"plop","val2":123"""))
    }

    @Test
    fun serialize2() {
        val json = obj.serialize()
        assertTrue(json.contains(""""val1":"plop","val2":123"""))
    }

    @Test
    fun serializeList() {
        val list = listOf(ObjTest("one", 1), ObjTest("two", 2))
        val json = list.serialize()
        assertTrue(json.contains(""""val1":"one","val2":1"""))
        assertTrue(json.contains(""""val1":"two","val2":2"""))
    }

    @Test
    fun serializeDate() {
        val objDate = ObjTestDate(DateTime.parse("2019-07-30T14:08:51.420108+04:00"))
        val json = objDate.serialize()
        assertTrue(json.contains(""""val1":"2019-07-30T10:08:51.420Z""""), json)
    }

    @Test
    fun deserialize() {
        val objDeserialized = serializer.deserialize<ObjTest>(objSerialized)
        assertEquals(obj.val1, objDeserialized!!.val1)
        assertEquals(obj.val2, objDeserialized.val2)
    }

    @Test
    fun deserializeWhitExtraField() {
        val objDeserialized = serializer.deserialize<ObjTest>(objSerializedWithExtra)
        assertEquals(obj.val1, objDeserialized!!.val1)
        assertEquals(obj.val2, objDeserialized.val2)
    }

    @Test
    fun deserializeUpdate() {
        val objDeserialized: ObjTest = serializer.deserialize(objSerializedUpdate, obj)
        assertTrue(obj === objDeserialized)
        assertEquals("update", objDeserialized.val1)
        assertEquals(123, objDeserialized.val2)
    }

    @Test
    fun deserializeUpdate2() {
        val objDeserialized = obj.deserialize(objSerializedUpdate)
        assertTrue(obj === objDeserialized)
        assertEquals("update", objDeserialized.val1)
        assertEquals(123, objDeserialized.val2)
    }
}