package fr.postgresjson.serializer

import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.IdEntity
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConnectionTest() {
    private lateinit var connection: Connection

    @BeforeEach
    fun before() {
        connection = Connection()
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
        assertTrue(objs !== null)
        assertTrue(objs is List<ObjTest2>)
        assertTrue(objs!!.size == 2)
        assertTrue(objs[0].id == 1)
        assertTrue(objs[0].test!!.id == 1)
    }
}

class ObjTest(var name: String): IdEntity()
class ObjTest2(var title: String, var test: ObjTest?): IdEntity()