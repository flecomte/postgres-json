package fr.postgresjson

import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.util.isCompleted
import fr.postgresjson.connexion.Requester
import fr.postgresjson.entity.IdEntity
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.CompletableFuture

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
            .selectOne(listOf("test", "plip"))

        assertEquals(objTest!!.id, 3)
        assertEquals(objTest.name, "test")
    }

    @Test
    fun callExecOnQuery() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val future: CompletableFuture<QueryResult> = Requester(getConnextion())
            .addQuery(resources)
            .getQuery("Test/selectOne")
            .exec()

        future.join()
        Assertions.assertTrue(future.isCompleted)
    }

    @Test
    fun callExecOnFunction() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val future: CompletableFuture<QueryResult> = Requester(getConnextion())
            .addFunction(resources)
            .getFunction("test_function")
            .exec(listOf("test", "plip"))

        future.join()
        Assertions.assertTrue(future.isCompleted)
    }
}