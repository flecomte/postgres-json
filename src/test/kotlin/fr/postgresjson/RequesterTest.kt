package fr.postgresjson

import fr.postgresjson.connexion.Paginated
import fr.postgresjson.connexion.Requester
import fr.postgresjson.entity.UuidEntity
import org.junit.Assert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class RequesterTest : TestAbstract() {
    class ObjTest(var name: String, id: UUID = UUID.fromString("5623d902-3067-42f3-bfd9-095dbb12c29f")) : UuidEntity(id)

    @Test
    fun `get query from file`() {
        val resources = this::class.java.getResource("/sql/query").toURI()
        val objTest: ObjTest? = Requester(connection)
            .addQuery(resources)
            .getQuery("selectOne")
            .selectOne()

        assertEquals(objTest!!.id, UUID.fromString("829b1a29-5db8-47f9-9562-961c561ac528"))
        assertEquals(objTest.name, "test")
    }

    @Test
    fun `get function from file`() {
        val resources = this::class.java.getResource("/sql/function/Test").toURI()
        val objTest: ObjTest? = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function")
            .selectOne(listOf("test", "plip"))

        assertEquals(objTest!!.id, UUID.fromString("457daad5-4f1b-4eb7-80ec-6882adb8cc7d"))
        assertEquals(objTest.name, "test")
    }

    @Test
    fun `call exec on query`() {
        val resources = this::class.java.getResource("/sql/query").toURI()
        val result = Requester(connection)
            .addQuery(resources)
            .getQuery("selectOne")
            .exec()

        assertEquals(1, result.rowsAffected)
    }

    @Test
    fun `call exec on function`() {
        val resources = this::class.java.getResource("/sql/function/Test").toURI()
        val result = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function")
            .exec(listOf("test", "plip"))

        assertEquals(1, result.rowsAffected)
    }

    @Test
    fun `call sendQuery on query with name`() {
        val resources = this::class.java.getResource("/sql/query").toURI()
        val result = Requester(connection)
            .addQuery(resources)
            .getQuery("DeleteTest")
            .sendQuery()

        assertEquals(0, result)
    }

    @Test
    fun `call sendQuery on function`() {
        val resources = this::class.java.getResource("/sql/function/Test").toURI()
        val result = Requester(connection)
            .addFunction(resources)
            .getFunction("function_void")
            .sendQuery(listOf("test"))

        assertEquals(0, result)
    }

    @Test
    fun `call selectOne on function`() {
        val resources = this::class.java.getResource("/sql/function/Test").toURI()
        val obj: ObjTest = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function")
            .selectOne(mapOf("name" to "myName"))!!

        assertEquals("myName", obj.name)
    }

    @Test
    fun `call selectOne on function with object`() {
        val resources = this::class.java.getResource("/sql/function/Test").toURI()
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
        val resources = this::class.java.getResource("/sql/query").toURI()
        val obj: ObjTest = Requester(connection)
            .addQuery(resources)
            .getQuery("selectOneWithParameters")
            .selectOne(mapOf("name" to "myName"))!!

        assertEquals("myName", obj.name)
    }

    @Test
    fun `call select (multiple) on function`() {
        val resources = this::class.java.getResource("/sql/function/Test").toURI()
        val obj: List<ObjTest>? = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function_multiple")
            .select(mapOf("name" to "myName"))

        assertEquals("myName", obj!![0].name)
    }

    @Test
    fun `call select paginated on query`() {
        val resources = this::class.java.getResource("/sql/query").toURI()
        val result: Paginated<ObjTest> = Requester(connection)
            .addQuery(resources)
            .getQuery("selectPaginated")
            .select(1, 2, mapOf("name" to "ff"))
        Assert.assertNotNull(result)
        Assert.assertEquals("ff", result.result[0].name)
        Assert.assertEquals("ff-2", result.result[1].name)
        Assert.assertEquals(10, result.total)
        Assert.assertEquals(0, result.offset)
    }

    @Test
    fun `call select paginated on function`() {
        val resources = this::class.java.getResource("/sql/function").toURI()
        val result: Paginated<ObjTest> = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function_paginated")
            .select(1, 2, mapOf("name" to "ff"))
        Assert.assertNotNull(result)
        Assert.assertEquals("ff", result.result[0].name)
        Assert.assertEquals("ff-2", result.result[1].name)
        Assert.assertEquals(10, result.total)
        Assert.assertEquals(0, result.offset)
    }

    @Test
    fun `call selectOne on query with extra parameter`() {
        val resources = this::class.java.getResource("/sql/query").toURI()
        val obj: ObjTest = Requester(connection)
            .addQuery(resources)
            .getQuery("selectOneWithParameters")
            .selectOne(mapOf("name" to "myName")) {
                assertEquals("myName", it!!.name)
                Assert.assertEquals("plop", rows[0].getString("other"))
            }!!

        assertEquals("myName", obj.name)
    }
}
