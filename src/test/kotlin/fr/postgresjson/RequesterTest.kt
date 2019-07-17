package fr.postgresjson

import fr.postgresjson.connexion.Paginated
import fr.postgresjson.connexion.Requester
import fr.postgresjson.entity.IdEntity
import org.junit.Assert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

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
        val result = Requester(getConnextion())
            .addQuery(resources)
            .getQuery("Test/selectOne")
            .exec()

        assertEquals(1, result.rowsAffected)
    }

    @Test
    fun `call exec on function`() {
        val resources = File(this::class.java.getResource("/sql/function").toURI())
        val result = Requester(getConnextion())
            .addFunction(resources)
            .getFunction("test_function")
            .exec(listOf("test", "plip"))

        assertEquals(1, result.rowsAffected)
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

    @Test
    fun `call select paginated on query`() {
        val resources = File(this::class.java.getResource("/sql/query").toURI())
        val result: Paginated<ObjTest> = Requester(getConnextion())
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
        val result: Paginated<ObjTest> = Requester(getConnextion())
            .addFunction(resources)
            .getFunction("test_function_paginated")
            .select(1, 2, mapOf("name" to "ff"))
        Assert.assertNotNull(result)
        Assert.assertEquals(result.result[0].name, "ff")
        Assert.assertEquals(result.result[1].name, "ff-2")
        Assert.assertEquals(result.total, 10)
        Assert.assertEquals(result.offset, 0)
    }
}