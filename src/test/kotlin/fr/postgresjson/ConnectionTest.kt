package fr.postgresjson

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.connexion.Connection.QueryError
import fr.postgresjson.connexion.Paginated
import fr.postgresjson.connexion.select
import fr.postgresjson.connexion.selectOne
import fr.postgresjson.entity.Parameter
import fr.postgresjson.entity.UuidEntity
import fr.postgresjson.serializer.deserialize
import fr.postgresjson.serializer.toTypeReference
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionTest : TestAbstract() {
    private class ObjTest(val name: String, id: UUID = UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00")) : UuidEntity(id)
    private class ObjTest2(val title: String, var test: ObjTest?) : UuidEntity()
    private class ObjTest3(val first: String, var second: String, var third: Int) : UuidEntity()
    private class ObjTestWithParameterObject(var first: ParameterObject, var second: ParameterObject) : UuidEntity()
    private class ParameterObject(var third: String) : Parameter

    @Test
    fun getObject() {
        val obj: ObjTest? = connection.selectOne("select to_json(a) from test a limit 1")
        assertTrue(obj is ObjTest)
        assertEquals(UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"), obj.id)
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
        assertEquals("myName", result.name)
    }

    @Test
    fun `test call request without args`() {
        val result: ObjTest? = connection.selectOne("select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', 'myName')", object : TypeReference<ObjTest>() {}) {
            assertEquals("myName", this.rows[0].getString(0)?.deserialize<ObjTest>()?.name)
        }
        assertNotNull(result)
        assertEquals("myName", result.name)
    }

    @Test
    fun `test call request return null`() {
        val result: ObjTest? = connection.selectOne("select null;", object : TypeReference<ObjTest>() {})
        assertNull(result)
    }

    @Test
    fun `test call request return nothing`() {
        val result: ObjTest? = connection.selectOne("select * from test where false;", object : TypeReference<ObjTest>() {})
        assertNull(result)
    }

    @Test
    fun callRequestWithArgsEntity() {
        val o = ObjTest("myName", id = UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00"))
        val obj: ObjTest? = connection.selectOne("select json_build_object('id', id, 'name', name) FROM json_to_record(?::json) as o(id uuid, name text);", listOf(o))
        assertNotNull(obj)
        assertEquals(UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00"), obj.id)
        assertEquals("myName", obj.name)
    }

    @Test
    fun `test update Entity`() {
        val obj = ObjTest("before", id = UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
        val objUpdated: ObjTest? = connection.update("select ?::jsonb || jsonb_build_object('name', 'after');", obj.toTypeReference(), obj)
        assertTrue(objUpdated is ObjTest)
        assertEquals(UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"), objUpdated.id)
        assertEquals("after", objUpdated.name)
    }

    @Test
    fun callExec() {
        val o = ObjTest("myName")
        val result = connection.exec("select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', ?::json->>'name')", listOf(o))
        assertEquals(1, result.rowsAffected)
    }

    @Test
    fun `select one with named parameters`() {
        val result: ObjTest3? = connection.selectOne(
            "SELECT json_build_object('first', :first::text, 'second', :second::text, 'third', :third::int)",
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
        val result: ObjTestWithParameterObject? = connection.selectOne(
            "SELECT json_build_object('first', :first::json, 'second', :second::json)",
            mapOf(
                "first" to ParameterObject("one"),
                "second" to ParameterObject("two")
            )
        )
        assertNotNull(result)
        assertEquals("one", result.first.third)
        assertEquals("two", result.second.third)
    }

    @Test
    fun `select with named parameters`() {
        val result: List<ObjTest3> = connection.select(
            """
            SELECT json_build_array(
                json_build_object('first', :first::text, 'second', :second::text, 'third', :third::int),
                json_build_object('first', :first::text, 'second', :second::text, 'third', :third::int)
            )
            """.trimIndent(),
            mapOf(
                "first" to "ff",
                "third" to 123,
                "second" to "sec"
            )
        )
        assertEquals("ff", result[0].first)
        assertEquals("sec", result[0].second)
        assertEquals(123, result[0].third)
    }

    @Test
    fun `select with named parameters as vararg of Pair`() {
        val result: List<ObjTest3> = connection.select(
            """
            SELECT json_build_array(
                json_build_object('first', :first::text, 'second', :second::text, 'third', :third::int),
                json_build_object('first', :first::text, 'second', :second::text, 'third', :third::int)
            )
            """.trimIndent(),
            "first" to "ff",
            "third" to 123,
            "second" to "sec"
        )
        assertEquals("ff", result[0].first)
        assertEquals("sec", result[0].second)
        assertEquals(123, result[0].third)
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
        assertEquals("ff", result.result[0].name)
        assertEquals("ff-2", result.result[1].name)
        assertEquals(10, result.total)
        assertEquals(0, result.offset)
    }

    @Test
    fun `test select paginated without result`() {
        val result: Paginated<ObjTest> = connection.select(
            """
            SELECT null, 
            10 as total
            LIMIT :limit 
            OFFSET :offset
            """.trimIndent(),
            1,
            2,
            object : TypeReference<List<ObjTest>>() {}
        )
        assertNotNull(result)
        assertTrue(result.result.isEmpty())
        assertEquals(0, result.result.size)
        assertEquals(10, result.total)
        assertEquals(0, result.offset)
    }

    @Test
    fun `test select paginated`() {
        val result: Paginated<ObjTest> = connection.select(
            """
            SELECT json_build_array(
                jsonb_build_object(
                    'name', :name::text,
                    'id', 'e9f9a0f0-237c-47cf-98c5-be353f2f2ce3'
                )
            ), 
            10 as total
            LIMIT :limit 
            OFFSET :offset
            """.trimIndent(),
            1,
            2,
            object : TypeReference<List<ObjTest>>() {},
            mapOf(
                "name" to "myName"
            )
        )
        assertNotNull(result)
        assertEquals("myName", result.result[0].name)
        assertEquals(1, result.result.size)
        assertEquals(10, result.total)
        assertEquals(0, result.offset)
    }

    @Test
    fun `test select paginated with no result`() {
        assertThrows<QueryError> {
            connection.select(
                """
                SELECT :name as name,
                10 as total
                LIMIT :limit 
                OFFSET :offset
                """.trimIndent(),
                100,
                10,
                object : TypeReference<List<ObjTest>>() {},
                mapOf(
                    "name" to "myName"
                )
            )
        }.run {
            assertNotNull(message)
            assertContains(message!!, "The query has no return")
        }
    }

    @Test
    fun `test select paginated with total was not integer`() {
        assertThrows<QueryError> {
            connection.select(
                """
                SELECT :name as name,
                'plop' as total
                LIMIT :limit 
                OFFSET :offset
                """.trimIndent(),
                1,
                10,
                object : TypeReference<List<ObjTest>>() {},
                mapOf(
                    "name" to "myName"
                )
            )
        }.run {
            assertNotNull(message)
            assertContains(message!!, """Column "total" must be an integer""")
        }
    }

    @Test
    fun `test select paginated without total`() {
        val exception = assertThrows<QueryError> {
            val result: Paginated<ObjTest> = connection.select(
                """
            SELECT null
            LIMIT :limit 
            OFFSET :offset
                """.trimIndent(),
                1,
                2,
                object : TypeReference<List<ObjTest>>() {}
            )
        }

        assertEquals(
            """
            The query not return the "total" column
            
              > :offset = 0, :limit = 2
              > SELECT null
              > LIMIT :limit 
              > OFFSET :offset
              > -----
              > ?column?
              > null
            """.trimIndent(),
            exception.message
        )
    }

    @Test
    fun `selectOne with extra parameters`() {
        val params: Map<String, Any?> = mapOf(
            "first" to "ff",
            "third" to 123,
            "second" to "sec"
        )
        val result: ObjTest3? = connection.selectOne(
            """
            SELECT json_build_object('first', :first::text, 'second', :second::text, 'third', :third::int), 'plop'::text as other
            """.trimIndent(),
            params
        ) {
            assertNotNull(it)
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
            selectOne<ObjTestWithParameterObject>(
                "SELECT json_build_object('first', :first::json, 'second', :second::json)",
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
