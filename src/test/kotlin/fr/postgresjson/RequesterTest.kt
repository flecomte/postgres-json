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

class RequesterTest: TestAbstract() {
    class ObjTest(var name:String): IdEntity(1)

    @Test
    fun `get query from file`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val objTest: ObjTest? = Requester(getConnextion())
            .addQuery(resources)
            .getQuery("Test/selectOne")
            .selectOne()

        assertEquals(objTest!!.id, 2)
        assertEquals(objTest.name, "test")
    }

    @Test
    fun `get function from file`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val objTest: ObjTest? = Requester(getConnextion())
            .addFunction(resources)
            .getFunction("test_function")
            .selectOne(listOf("test", "plip"))

        assertEquals(objTest!!.id, 3)
        assertEquals(objTest.name, "test")
    }

    @Test
    fun `call exec on query`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val future: CompletableFuture<QueryResult> = Requester(getConnextion())
            .addQuery(resources)
            .getQuery("Test/selectOne")
            .exec()

        future.join()
        Assertions.assertTrue(future.isCompleted)
    }

    @Test
    fun `call exec on function`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val future: CompletableFuture<QueryResult> = Requester(getConnextion())
            .addFunction(resources)
            .getFunction("test_function")
            .exec(listOf("test", "plip"))

        future.join()
        Assertions.assertTrue(future.isCompleted)
    }

    @Test
    fun `call selectOne on function`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val obj: ObjTest = Requester(getConnextion())
            .addFunction(resources)
            .getFunction("test_function")
            .selectOne(mapOf("name" to "myName"))!!

        assertEquals("myName", obj.name)
    }

    @Test
    fun `call selectOne on query`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val obj: ObjTest = Requester(getConnextion())
            .addQuery(resources)
            .getQuery("Test/selectOneWithParameters")
            .selectOne(mapOf("name" to "myName"))!!

        assertEquals("myName", obj.name)
    }

    @Test
    fun `call select (multiple) on function`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val obj: List<ObjTest>? = Requester(getConnextion())
            .addFunction(resources)
            .getFunction("test_function_multiple")
            .select(mapOf("name" to "myName"))

        assertEquals("myName", obj!![0].name)
    }
}