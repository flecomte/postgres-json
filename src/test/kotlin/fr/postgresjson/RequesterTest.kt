package fr.postgresjson

import fr.postgresjson.connexion.Paginated
import fr.postgresjson.connexion.Requester
import fr.postgresjson.entity.IdEntity
import org.junit.Assert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File

class RequesterTest: TestAbstract() {
    class ObjTest(var name:String): IdEntity(1)

    @Test
    fun `get query from file`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val objTest: ObjTest? = Requester(connection)
            .addQuery(resources)
            .getQuery("Test/selectOne")
            .selectOne()

        assertEquals(objTest!!.id, 2)
        assertEquals(objTest.name, "test")
    }

    @Test
    fun `get function from file`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val objTest: ObjTest? = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function")
            .selectOne(listOf("test", "plip"))

        assertEquals(objTest!!.id, 3)
        assertEquals(objTest.name, "test")
    }

    @Test
    fun `call exec on query`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val result = Requester(connection)
            .addQuery(resources)
            .getQuery("Test/selectOne")
            .exec()

        assertNotNull(result.getString(1))
    }

    @Test
    fun `call exec on function`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val result = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function")
            .exec(listOf("test", "plip"))

        assertNotNull(result.getString(1))
    }

    @Test
    fun `call sendQuery on query`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val result = Requester(connection)
            .addQuery(resources)
            .getQuery("Test/exec")
            .sendQuery()

        assertEquals(0, result)
    }

    @Test
    fun `call sendQuery on function`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val result = Requester(connection)
            .addFunction(resources)
            .getFunction("function_void")
            .sendQuery(listOf("test"))

        assertEquals(0, result)
    }

    @Test
    fun `call selectOne on function`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val obj: ObjTest = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function")
            .selectOne(mapOf("name" to "myName"))!!

        assertEquals("myName", obj.name)
    }

    @Test
    fun `call selectOne on function with object`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val obj2 = ObjTest("original")
        val obj: ObjTest = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function_object")
            .selectOne("resource" to obj2)!!

        assertEquals("changedName", obj.name)
        assertEquals("changedName", obj2.name)
    }

    @Test
    fun `call selectOne on query`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val obj: ObjTest = Requester(connection)
            .addQuery(resources)
            .getQuery("Test/selectOneWithParameters")
            .selectOne(mapOf("name" to "myName"))!!

        assertEquals("myName", obj.name)
    }

    @Test
    fun `call select (multiple) on function`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val obj: List<ObjTest>? = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function_multiple")
            .select(mapOf("name" to "myName"))

        assertEquals("myName", obj!![0].name)
    }

    @Test
    fun `call select paginated on query`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val result: Paginated<ObjTest> = Requester(connection)
            .addQuery(resources)
            .getQuery("Test/selectPaginated")
            .select(1, 2, mapOf("name" to "ff"))
        Assert.assertNotNull(result)
        Assert.assertEquals(result.result[0].name, "ff")
        Assert.assertEquals(result.result[1].name, "ff-2")
        Assert.assertEquals(result.total, 10)
        Assert.assertEquals(result.offset, 0)
    }

    @Test
    fun `call select paginated on function`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val result: Paginated<ObjTest> = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function_paginated")
            .select(1, 2, mapOf("name" to "ff"))
        Assert.assertNotNull(result)
        Assert.assertEquals(result.result[0].name, "ff")
        Assert.assertEquals(result.result[1].name, "ff-2")
        Assert.assertEquals(result.total, 10)
        Assert.assertEquals(result.offset, 0)
    }

    @Test
    fun `call selectOne on query with extra parameter`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val obj: ObjTest = Requester(connection)
            .addQuery(resources)
            .getQuery("Test/selectOneWithParameters")
            .selectOne(mapOf("name" to "myName")) {
                assertEquals("myName", it!!.name)
                Assert.assertEquals("plop", getString("other"))
            }!!

        assertEquals("myName", obj.name)
    }
}