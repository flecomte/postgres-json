package fr.postgresjson

import fr.postgresjson.serializer.Serializer
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SerializerTest {
    private class ObjTest(var val1: String, var val2: Int,val  id: UUID = UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
    private class ObjTestDate(var val1: DateTime, val id: UUID = UUID.fromString("829b1a29-5db8-47f9-9562-961c561ac528"))

    private val serializer = Serializer()

    private val objSerialized: String = """{"val1":"plop","val2":123,"id":"829b1a29-5db8-47f9-9562-961c561ac528"}"""
    private val objSerializedWithExtra: String = """{"val1":"plop","val2":123,"id":"829b1a29-5db8-47f9-9562-961c561ac528","toto":"tata"}"""
    private lateinit var obj: ObjTest

    @BeforeEach
    fun before() {
        obj = ObjTest("plop", 123, UUID.fromString("829b1a29-5db8-47f9-9562-961c561ac528"))
    }

    @Test
    fun serialize() {
        val json = serializer.serialize(obj)
        assertTrue(json.contains(""""val1":"plop","val2":123"""))
    }

    @Test
    fun serializeList() {
        val list = listOf(ObjTest("one", 1), ObjTest("two", 2))
        val json = serializer.serialize(list)
        assertTrue(json.contains(""""val1":"one","val2":1"""))
        assertTrue(json.contains(""""val1":"two","val2":2"""))
    }

    @Test
    fun serializeDate() {
        val objDate = ObjTestDate(DateTime.parse("2019-07-30T14:08:51.420108+04:00"))
        val json = serializer.serialize(objDate)
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
}
