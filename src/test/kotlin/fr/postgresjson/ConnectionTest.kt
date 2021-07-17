package fr.postgresjson

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.connexion.Paginated
import fr.postgresjson.connexion.select
import fr.postgresjson.connexion.selectOne
import fr.postgresjson.entity.Parameter
import fr.postgresjson.entity.UuidEntity
import fr.postgresjson.serializer.deserialize
import fr.postgresjson.serializer.toTypeReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionTest : TestAbstract() {
    private class ObjTest(val name: String, id: UUID = UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00")) : UuidEntity(id)
    private class ObjTest2(val title: String, var test: ObjTest?) : UuidEntity()
    private class ObjTest3(val first: String, var seconde: String, var third: Int) : UuidEntity()
    private class ObjTestWithParameterObject(var first: ParameterObject, var seconde: ParameterObject) : UuidEntity()
    private class ParameterObject(var third: String) : Parameter

    @Test
    fun getObject() {
        val obj: ObjTest? = connection.selectOne("select to_json(a) from test a limit 1")
        assertTrue(obj is ObjTest)
        assertTrue(obj!!.id == UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
    }

    @Test
    fun getExistingObject() {
        val objs: List<ObjTest2> = connection.select(
            """
            select
                json_agg(j)
                FROM (
                SELECT
                    t.id, t.title,
                    t2 as test
                from test2 t
                JOIN test t2 ON t.test_id = t2.id
            ) j;
            """.trimIndent()
        )
        assertNotNull(objs)
        assertEquals(objs.size, 2)
        assertEquals(objs[0].id, UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
        assertEquals(objs[0].test!!.id, UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
    }

    @Test
    fun `test call request with args`() {
        val result: ObjTest? = connection.selectOne("select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', ?::text)", listOf("myName"))
        assertNotNull(result)
        assertEquals("myName", result!!.name)
    }

    @Test
    fun `test call request without args`() {
        val result: ObjTest? = connection.selectOne("select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', 'myName')", object : TypeReference<ObjTest>() {}) {
            assertEquals("myName", this.rows[0].getString(0)?.deserialize<ObjTest>()?.name)
        }
        assertNotNull(result)
        assertEquals("myName", result!!.name)
    }

    @Test
    fun `test call request return null`() {
        val result: ObjTest? = connection.selectOne("select null;", object : TypeReference<ObjTest>() {})
        assertNull(result)
    }

    @Test
    fun callRequestWithArgsEntity() {
        val o = ObjTest("myName", id = UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00"))
        val obj: ObjTest? = connection.selectOne("select json_build_object('id', id, 'name', name) FROM json_to_record(?::json) as o(id uuid, name text);", listOf(o))
        assertNotNull(obj)
        assertTrue(obj is ObjTest)
        assertEquals(obj!!.id, UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00"))
        assertEquals(obj.name, "myName")
    }

    @Test
    fun `test update Entity`() {
        val obj = ObjTest("before", id = UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
        val objUpdated: ObjTest? = connection.update("select ?::jsonb || jsonb_build_object('name', 'after');", obj.toTypeReference(), obj)
        assertTrue(objUpdated is ObjTest)
        assertTrue(objUpdated!!.id == UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
        assertTrue(objUpdated.name == "after")
    }

    @Test
    fun callExec() {
        val o = ObjTest("myName")
        val result = connection.exec("select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', ?::json->>'name')", listOf(o))
        Assertions.assertEquals(1, result.rowsAffected)
    }

    @Test
    fun `select one with named parameters`() {
        val result: ObjTest3? = connection.selectOne(
            "SELECT json_build_object('first', :first::text, 'seconde', :seconde::text, 'third', :third::int)",
            mapOf(
                "first" to "ff",
                "seconde" to "sec",
                "third" to 123
            )
        )
        assertEquals(result!!.first, "ff")
        assertEquals(result.seconde, "sec")
        assertEquals(result.third, 123)
    }

    @Test
    fun `select one with named parameters object`() {
        val result: ObjTestWithParameterObject? = connection.selectOne(
            "SELECT json_build_object('first', :first::json, 'seconde', :seconde::json)",
            mapOf(
                "first" to ParameterObject("one"),
                "seconde" to ParameterObject("two")
            )
        )
        assertEquals("one", result!!.first.third)
        assertEquals("two", result.seconde.third)
    }

    @Test
    fun `select with named parameters`() {
        val params: Map<String, Any?> = mapOf(
            "first" to "ff",
            "third" to 123,
            "seconde" to "sec"
        )
        val result: List<ObjTest3> = connection.select(
            """
            SELECT json_build_array(
                json_build_object('first', :first::text, 'seconde', :seconde::text, 'third', :third::int),
                json_build_object('first', :first::text, 'seconde', :seconde::text, 'third', :third::int)
            )
            """.trimIndent(),
            params
        )
        assertEquals(result[0].first, "ff")
        assertEquals(result[0].seconde, "sec")
        assertEquals(result[0].third, 123)
    }

    @Test
    fun `selectOne with named parameters`() {
        val params: Map<String, Any?> = mapOf(
            "first" to "ff",
            "third" to 123,
            "seconde" to "sec"
        )
        val result: ObjTest3? = connection.selectOne(
            """
            SELECT json_build_object('first', :first::text, 'seconde', :seconde::text, 'third', :third::int)
            """.trimIndent(),
            params
        )
        assertNotNull(result)
        assertEquals(result!!.first, "ff")
        assertEquals(result.seconde, "sec")
        assertEquals(result.third, 123)
    }

    @Test
    fun `select paginated`() {
        val result: Paginated<ObjTest> = connection.select(
            """
            SELECT json_build_array(
                json_build_object('id', '417aaa7e-7bc6-49b7-9fe8-6c8433b3f430', 'name', :name::text),
                json_build_object('id', 'abd46e7a-e749-4ce4-8361-e7b64da89da6', 'name', :name::text || '-2')
            ), 10 as total
            LIMIT :limit OFFSET :offset
            """.trimIndent(),
            1,
            2,
            mapOf("name" to "ff")

        )
        assertNotNull(result)
        assertEquals(result.result[0].name, "ff")
        assertEquals(result.result[1].name, "ff-2")
        assertEquals(result.total, 10)
        assertEquals(result.offset, 0)
    }

    @Test
    fun `selectOne with extra parameters`() {
        val params: Map<String, Any?> = mapOf(
            "first" to "ff",
            "third" to 123,
            "seconde" to "sec"
        )
        val result: ObjTest3? = connection.selectOne(
            """
            SELECT json_build_object('first', :first::text, 'seconde', :seconde::text, 'third', :third::int), 'plop'::text as other
            """.trimIndent(),
            params
        ) {
            assertEquals("ff", it!!.first)
            assertEquals("plop", rows[0].getString("other"))
        }
        assertNotNull(result)
        assertEquals("ff", result!!.first)
        assertEquals("sec", result.seconde)
        assertEquals(123, result.third)
    }
}
