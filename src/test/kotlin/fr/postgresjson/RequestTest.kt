package fr.postgresjson

import fr.postgresjson.connexion.Requester
import fr.postgresjson.entity.IdEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class RequestTest: TestAbstract() {
    class ObjTest(var name:String): IdEntity(1)

    @Test
    fun getQueryFromFile() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val objTest: ObjTest? = Requester(getConnextion())
            .addQuery(resources)
            .getQuery("Test/selectOne")
            .selectOne()
        assertEquals(objTest!!.id, 2)
        assertEquals(objTest.name, "test")
    }

    @Test
    fun getFunctionFromFile() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val objTest: ObjTest? = Requester(getConnextion())
            .addFunction(resources)
            .getFunction("test_function")
            .selectOne(listOf("ploop", "plip"))
        assertEquals(objTest!!.id, 3)
        assertEquals(objTest.name, "test")
    }
}