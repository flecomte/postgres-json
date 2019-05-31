package fr.postgresjson.serializer

import fr.postgresjson.entity.UuidEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SerializerTest {
    class ObjTest(var val1: String, var val2: Int) : UuidEntity()

    private val serializer = Serializer()

    private val objSerialized = """{"val1":"plop","val2":123,"id":"1362a162-df75-4995-ab46-4ad55fa07de2"}"""
    private val objSerializedUpdate = """{"val1":"update","val2":123}"""
    private lateinit var obj: ObjTest

    @BeforeEach
    fun before() {
        obj = ObjTest("plop", 123)
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
        val objDeserialized: ObjTest = serializer.deserialize(objSerialized)
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
        val objDeserialized: ObjTest = obj.deserialize(objSerializedUpdate)
        assertTrue(obj === objDeserialized)
        assertEquals("update", objDeserialized.val1)
        assertEquals(123, objDeserialized.val2)
    }
}