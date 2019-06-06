package fr.postgresjson

import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.IdEntity
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionTest() {
    private class ObjTest(var name: String): IdEntity()
    private class ObjTest2(var title: String, var test: ObjTest?): IdEntity()

    private lateinit var connection: Connection

    fun getConnextion(): Connection {
        return Connection(database = "test", username = "test", password = "test")
    }

    @BeforeAll
    fun beforeAll() {
        val initSQL = File(this::class.java.getResource("/fixtures/init.sql").toURI())
        val promise = getConnextion().connect().sendQuery(initSQL.readText())
        promise.join()
    }

    @BeforeEach
    fun before() {
        connection = getConnextion()
    }

    @AfterAll
    fun afterAll() {
        val downSQL = File(this::class.java.getResource("/fixtures/down.sql").toURI())
        getConnextion().connect().sendQuery(downSQL.readText()).join()
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