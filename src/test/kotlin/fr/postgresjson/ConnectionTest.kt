package fr.postgresjson

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.connexion.DataNotFoundException
import fr.postgresjson.connexion.SqlSerializable
import fr.postgresjson.connexion.execute
import fr.postgresjson.serializer.deserialize
import fr.postgresjson.serializer.toTypeReference
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.reflect.full.hasAnnotation
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionTest : TestAbstract() {
    @SqlSerializable
    private class ObjTest(val name: String, val id: UUID = UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00"))
    @SqlSerializable
    private class ObjTest2(val id: UUID, val title: String, var test: ObjTest?)
    @SqlSerializable
    private class ObjTest3(val id: UUID, val first: String, var second: String, var third: Int)
    @SqlSerializable
    private class ObjTestWithParameterObject(val id: UUID, var first: ParameterObject, var second: ParameterObject)
    @SqlSerializable
    private class ParameterObject(var third: String)
    private class ObjTest4

    @Test
    fun serializable() {
        assertTrue(ObjTest("plop")::class.hasAnnotation<SqlSerializable>())
        assertFalse(ObjTest4()::class.hasAnnotation<SqlSerializable>())
    }

    @Test
    fun getObject() {
        val obj: ObjTest? = connection.execute("select to_json(a) from test a limit 1")
        assertNotNull(obj)
        assertEquals(UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"), obj.id)
    }

    @Test
    fun getExistingObject() {
        val objs: List<ObjTest2>? = connection.execute<List<ObjTest2>>(
            """
            select
                json_agg(j)
                FROM (
                SELECT
                    t.id, 
                    t.title,
                    t2 as test
                from test2 t
                JOIN test t2 ON t.test_id = t2.id
            ) j;
            """.trimIndent()
        )
        assertNotNull(objs)
        assertEquals(objs.size, 2)
        assertEquals(objs[0].id, UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
        assertEquals(objs[0].title, "plop")
        assertEquals(objs[0].test!!.id, UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
    }

    @Test
    fun `test call request with args`() {
        val result: ObjTest? = connection.execute(
            "select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', ?::text)",
            listOf("myName")
        )
        assertNotNull(result)
        assertEquals("myName", result.name)
    }

    @Test
    fun `test call request without args`() {
        val result: ObjTest? = connection.execute(
            "select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', 'myName')",
            object : TypeReference<ObjTest>() {}
        ) {
            assertEquals("myName", this.deserialize<ObjTest>()?.name)
        }
        assertNotNull(result)
        assertEquals("myName", result.name)
    }

    @Test
    fun `test call request return null`() {
        val result: ObjTest? = connection.execute("select null;", object : TypeReference<ObjTest>() {})
        assertNull(result)
    }

    @Test
    fun `test call request return nothing`() {
        val e = assertThrows<DataNotFoundException> {
            connection.execute("select * from test where false;", object : TypeReference<ObjTest>() {})
        }
        assertEquals("No data return for the query", e.message)
        assertEquals("select * from test where false;", e.queryExecuted)
    }

    @Test
    fun callRequestWithArgsEntity() {
        val o = ObjTest("myName", id = UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00"))
        val obj: ObjTest? = connection.execute(
            "select json_build_object('id', id, 'name', name) FROM json_to_record(?::json) as o(id uuid, name text);",
            listOf(o)
        )
        assertNotNull(obj)
        assertEquals(UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00"), obj.id)
        assertEquals("myName", obj.name)
    }

    @Test
    fun `test update Entity`() {
        val obj = ObjTest("before", id = UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
        val objUpdated: ObjTest? = connection.execute(
            "select ?::jsonb || jsonb_build_object('name', 'after');",
            obj.toTypeReference(), listOf(obj)
        )
        assertNotNull(objUpdated)
        assertEquals(UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"), objUpdated.id)
        assertEquals("after", objUpdated.name)
    }

    @Test
    fun `test update Entity with vararg`() {
        val obj = ObjTest("before", id = UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
        val objUpdated: ObjTest? = connection.execute(
            "select :obj::jsonb || jsonb_build_object('name', 'after');",
            obj.toTypeReference(),
            "obj" to obj
        )
        assertNotNull(objUpdated)
        assertEquals(UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"), objUpdated.id)
        assertEquals("after", objUpdated.name)
    }

    @Test
    fun callExec() {
        val o = ObjTest("myName")
        val result = connection.exec(
            "select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', ?::json->>'name')",
            listOf(o)
        )
        assertEquals(1, result.rowsAffected)
    }

    @Test
    fun `select one with named parameters`() {
        val result: ObjTest3? = connection.execute(
            """
            SELECT json_build_object(
                'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                'first', :first::text, 
                'second', :second::text, 
                'third', :third::int
            )
            """.trimIndent(),
            mapOf(
                "first" to "ff",
                "second" to "sec",
                "third" to 123
            )
        )
        assertNotNull(result)
        assertEquals("ff", result.first)
        assertEquals("sec", result.second)
        assertEquals(123, result.third)
    }

    @Test
    fun `select one with named parameters object`() {
        val result: ObjTestWithParameterObject? = connection.execute(
            """
            SELECT json_build_object(
                'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                'first', :first::json, 
                'second', :second::json
            )
            """.trimIndent(),
            mapOf(
                "first" to ParameterObject("one"),
                "second" to ParameterObject("two")
            )
        )
        assertNotNull(result)
        assertEquals("bf0e5605-3a8f-4db9-8b98-c8e0691dd576", result.id.toString())
        assertEquals("one", result.first.third)
        assertEquals("two", result.second.third)
    }

    @Test
    fun `select with named parameters`() {
        val result: List<ObjTest3>? = connection.execute(
            """
            SELECT json_build_array(
                json_build_object(
                    'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                    'first', :first::text, 
                    'second', :second::text, 
                    'third', :third::int
                ),
                json_build_object(
                    'id', 'ce9ae3c9-dc0e-4561-a168-811b996d913e', 
                    'first', :first::text, 
                    'second', :second::text, 
                    'third', :third::int
                )
            )
            """.trimIndent(),
            mapOf(
                "first" to "ff",
                "third" to 123,
                "second" to "sec"
            )
        )
        assertNotNull(result)
        assertEquals("bf0e5605-3a8f-4db9-8b98-c8e0691dd576", result[0].id.toString())
        assertEquals("ff", result[0].first)
        assertEquals("sec", result[0].second)
        assertEquals(123, result[0].third)
    }

    @Test
    fun `select with named parameters as vararg of Pair`() {
        val result: List<ObjTest3>? = connection.execute(
            """
            SELECT json_build_array(
                json_build_object('id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 'first', :first::text, 'second', :second::text, 'third', :third::int),
                json_build_object('id', '0c9d55d2-f69a-4750-a278-fac821774276', 'first', :first::text, 'second', :second::text, 'third', :third::int)
            )
            """.trimIndent(),
            "first" to "ff",
            "third" to 123,
            "second" to "sec"
        )
        assertNotNull(result)
        assertEquals("bf0e5605-3a8f-4db9-8b98-c8e0691dd576", result[0].id.toString())
        assertEquals("ff", result[0].first)
        assertEquals("sec", result[0].second)
        assertEquals(123, result[0].third)
    }

    @Test
    fun `execute with extra parameters`() {
        val params: Map<String, Any?> = mapOf(
            "first" to "ff",
            "third" to 123,
            "second" to "sec"
        )
        val result: ObjTest3? = connection.execute(
            """
            SELECT json_build_object(
                'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                'first', :first::text, 
                'second', :second::text, 
                'third', :third::int
            ), 'plop'::text as other
            """.trimIndent(),
            params
        ) {
            assertNotNull(it)
            assertEquals("bf0e5605-3a8f-4db9-8b98-c8e0691dd576", it.id.toString())
            assertEquals("ff", it.first)
            assertEquals("plop", rows[0].getString("other"))
        }
        assertNotNull(result)
        assertEquals("ff", result.first)
        assertEquals("sec", result.second)
        assertEquals(123, result.third)
    }

    @Test
    fun `test exec without parameters`() {
        connection.exec("select 42, 'hello';").run {
            assertEquals(42, rows[0].getInt(0))
            assertEquals("hello", rows[0].getString(1))
        }
    }

    @Test
    fun `test exec with one object as parameter`() {
        val obj = ObjTest("myName", UUID.fromString("c606e216-53b3-43c8-a900-e727cb4a017c"))
        connection.exec("select ?::jsonb->>'name'", obj).run {
            assertEquals("myName", rows[0].getString(0))
        }
    }

    @Test
    fun `select one in transaction`() {
        connection.inTransaction {
            execute<ObjTestWithParameterObject>(
                """
                SELECT json_build_object(
                    'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                    'first', :first::json, 
                    'second', :second::json
                    )
                """.trimIndent(),
                mapOf(
                    "first" to ParameterObject("one"),
                    "second" to ParameterObject("two")
                )
            ).let { result ->
                assertNotNull(result)
                assertEquals("one", result.first.third)
                assertEquals("two", result.second.third)
            }
        }
    }
}
