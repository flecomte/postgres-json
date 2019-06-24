package fr.postgresjson

import com.github.jasync.sql.db.util.isCompleted
import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.IdEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionTest(): TestAbstract() {
    private class ObjTest(var name: String): IdEntity()
    private class ObjTest2(var title: String, var test: ObjTest?): IdEntity()

    private lateinit var connection: Connection

    @BeforeEach
    fun before() {
        connection = getConnextion()
    }

    @Test
    fun getObject() {
        val obj: ObjTest? = connection.selectOne<Int?, ObjTest>("select to_json(a) from test a limit 1")
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
        """
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
        assertTrue(obj !== null)
        assertTrue(obj is ObjTest)
        assertTrue(obj!!.id == 88)
        assertTrue(obj.name == "myName")
    }

    @Test
    fun callExec() {
        val o = ObjTest("myName")
        val future = connection.exec("select json_build_object('id', 1, 'name', ?::json->>'name')", listOf(o))
        future.join()
        assertTrue(future.isCompleted)
    }
}