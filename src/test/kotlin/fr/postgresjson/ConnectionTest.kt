package fr.postgresjson

import fr.postgresjson.connexion.Connection
import fr.postgresjson.connexion.Paginated
import fr.postgresjson.entity.IdEntity
import org.junit.Assert.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionTest(): TestAbstract() {
    private class ObjTest(var name: String): IdEntity()
    private class ObjTest2(var title: String, var test: ObjTest?): IdEntity()
    private class ObjTest3(var first: String, var seconde: String, var third: Int): IdEntity()

    private lateinit var connection: Connection

    @BeforeEach
    fun before() {
        connection = getConnextion()
    }

    @Test
    fun getObject() {
        val obj: ObjTest? = connection.selectOne("select to_json(a) from test a limit 1")
        assertTrue(obj is ObjTest)
        assertTrue(obj!!.id == 1)
    }

    @Test
    fun getExistingObject() {
        val objs: List<ObjTest2> = connection.select("""
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
        assertTrue(objs is List<ObjTest2>)
        assertEquals(objs!!.size, 2)
        assertEquals(objs[0].id, 1)
        assertEquals(objs[0].test!!.id, 1)
    }

    @Test
    fun callRequestWithArgs() {
        val result: ObjTest? = connection.selectOne("select json_build_object('id', 1, 'name', ?::text)", listOf("myName"))
        assertNotNull(result)
        assertEquals("myName", result!!.name)
    }

    @Test
    fun callRequestWithArgsEntity() {
        val o = ObjTest("myName")
        o.id = 88
        val obj: ObjTest? = connection.selectOne("select json_build_object('id', id, 'name', name) FROM json_to_record(?::json) as o(id int, name text);", listOf(o))
        assertNotNull(obj)
        assertTrue(obj is ObjTest)
        assertEquals(obj!!.id, 88)
        assertEquals(obj.name, "myName")
    }

    @Test
    fun callExec() {
        val o = ObjTest("myName")
        val result = connection.exec("select json_build_object('id', 1, 'name', ?::json->>'name')", listOf(o))
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
                json_build_object('id', 3, 'name', :name::text),
                json_build_object('id', 4, 'name', :name::text || '-2')
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
}