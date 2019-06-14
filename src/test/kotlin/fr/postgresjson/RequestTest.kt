package fr.postgresjson

import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.IdEntity
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class RequestTest {
    class ObjTest(var name:String): IdEntity(1)

    @Test
    fun getRequestFromFile() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val objTest: ObjTest? = Connection(queriesDirectory = resources).getQuery("Test/selectOne").selectOne()
        assertTrue(objTest!!.id == 2)
        assertTrue(objTest.name == "test")
    }

    @Test
    fun getRequestFromFunction() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val objTest: ObjTest? = Connection(functionsDirectory = resources).getFunction("test_function").selectOne()
        assertTrue(objTest!!.id == 2)
        assertTrue(objTest.name == "test")
    }
}