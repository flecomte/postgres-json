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
        val objTest: ObjTest? = Connection(queriesDirectory = resources).selectOne("Test", "test")
        assertTrue(objTest!!.id == 2)
        assertTrue(objTest.name == "test")
    }
}