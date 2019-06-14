package fr.postgresjson

import fr.postgresjson.entity.IdEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SerializerTest: TestAbstract() {
    private class ObjTest(var val1: String, var val2: Int) : IdEntity(1)

    private val serializer = Serializer()

    private val objSerialized: String = """{"val1":"plop","val2":123,"id":2}"""
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
        assertTrue(json.contains("\"val1\":\"plop\",\"val2\":123"))
    }

    @Test
    fun serialize2() {
        val json = obj.serialize()
        assertTrue(json.contains("\"val1\":\"plop\",\"val2\":123"))
    }

    @Test
    fun deserialize() {
        val objDeserialized = serializer.deserialize<Int?, ObjTest>(objSerialized)
        assertEquals(obj.val1, objDeserialized.val1)
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