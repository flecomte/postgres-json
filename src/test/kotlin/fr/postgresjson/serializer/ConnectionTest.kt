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
        val obj: ObjTest? = connection.execute<Int?, ObjTest>("select to_json(a) from test a limit 1")
        assertTrue(obj is ObjTest)
    }

//    @Test
//    fun getExistingObject() {
//        val obj: ObjTest? = connection.execute<Int?, ObjTest>("select to_json(a) from test a limit 1")
//        assertTrue(obj is ObjTest)
//    }
}

class ObjTest(var name: String): IdEntity()