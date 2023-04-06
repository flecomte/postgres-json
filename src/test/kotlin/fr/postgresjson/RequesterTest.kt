package fr.postgresjson

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.connexion.Connection.QueryError
import fr.postgresjson.connexion.Requester
import fr.postgresjson.connexion.Requester.NoFunctionDefined
import fr.postgresjson.connexion.Requester.NoQueryDefined
import fr.postgresjson.connexion.SqlSerializable
import fr.postgresjson.connexion.execute
import fr.postgresjson.serializer.deserialize
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RequesterTest : TestAbstract() {
    @SqlSerializable
    class ObjTest(val name: String, val id: UUID = UUID.fromString("5623d902-3067-42f3-bfd9-095dbb12c29f"))

    @Test
    fun `requester constructor empty`() {
        val resources = this::class.java.getResource("/sql/function/Test")!!.toURI()
        val name: String = Requester(connection)
            .apply { addFunctions(resources) }
            .getFunction("test_function")
            .name

        assertEquals("test_function", name)
    }

    @Test
    fun `requester constructor function directory`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val name: String = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function")
            .name

        assertEquals("test_function", name)
    }

    @Test
    fun `requester constructor query directory`() {
        val resources = this::class.java.getResource("/sql/query/Test")?.toURI()
        val name: String = Requester(connection, queriesDirectory = resources)
            .getQuery("DeleteTest")
            .name

        assertEquals("DeleteTest", name)
    }

    @Test
    fun `function toString`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val name: String = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function")
            .toString()

        assertEquals("test_function", name)
    }

    @Test
    fun `add function as string`() {
        val sql = """
            CREATE OR REPLACE FUNCTION test_function (name text default 'plop', IN hi text default 'hello', out result json)
            LANGUAGE plpgsql
            AS
            $$
            BEGIN
                result = json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name);
            END;
            $$
        """.trimIndent()
        val name: String = Requester(connection)
            .apply { addFunction(sql) }
            .getFunction("test_function")
            .name

        assertEquals("test_function", name)
    }

    @Test
    fun `add query from string`() {
        val result: Int? = Requester(connection)
            .apply { addQuery("simpleTest", "select 42;") }
            .getQuery("simpleTest")
            .exec()
            .rows[0].getInt(0)

        assertNotNull(result)
        assertEquals(42, result)
    }

    @Test
    fun `get query from file`() {
        val resources = this::class.java.getResource("/sql/query")!!.toURI()
        val objTest: ObjTest? = Requester(connection)
            .apply { addQuery(resources) }
            .getQuery("selectOne")
            .execute()

        assertNotNull(objTest)
        assertEquals(objTest.id, UUID.fromString("829b1a29-5db8-47f9-9562-961c561ac528"))
        assertEquals("test", objTest.name)
    }

    @Test
    fun `get query from file with wrong name throw exception`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        assertThrows(NoQueryDefined::class.java) {
            Requester(connection, queriesDirectory = resources)
                .getQuery("wrongName")
        }
    }

    @Test
    fun `get queries from file`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        val name: String = Requester(connection, queriesDirectory = resources)
            .getQueries()[0].name

        assertEquals(name, "DeleteTest")
    }

    @Test
    fun `get function from file with wrong name throw exception`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        assertThrows(NoFunctionDefined::class.java) {
            Requester(connection, functionsDirectory = resources)
                .getFunction("wrongName")
        }
    }

    @Test
    fun `get function from file`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val objTest: ObjTest? = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function")
            .execute(listOf("test", "plip"))

        assertNotNull(objTest)
        assertEquals(objTest.id, UUID.fromString("457daad5-4f1b-4eb7-80ec-6882adb8cc7d"))
        assertEquals("test", objTest.name)
    }

    @Test
    fun `call exec on query`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        val result = Requester(connection, queriesDirectory = resources)
            .getQuery("selectOne")
            .exec()

        assertEquals(1, result.rowsAffected)
    }

    @Test
    fun `call exec on query with a list of arguments`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        val result = Requester(connection, queriesDirectory = resources)
            .getQuery("selectOneWithParameters")
            .exec(listOf("myName"))

        assertEquals("myName", result.deserialize<ObjTest>()?.name)
    }

    @Test
    fun `call exec on function`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val result = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function")
            .exec(listOf("test", "plip"))

        assertEquals(1, result.rowsAffected)
    }

    @Test
    fun `call exec on query with name`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        val result = Requester(connection, queriesDirectory = resources)
            .getQuery("DeleteTest")
            .exec()

        assertEquals(0, result.rowsAffected)
    }

    @Test
    fun `call sendQuery with same name of arguments`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        Requester(connection, queriesDirectory = resources)
            .getQuery("selectMultipleWithSameArgs")
            .sendQuery("name" to "myName").run {
                assertEquals("myName", rows[0].getString("firstName"))
                assertEquals("myName", rows[0].getString("secondName"))
            }
    }

    @Test
    fun `call sendQuery with same name of arguments as list`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        Requester(connection, queriesDirectory = resources)
            .getQuery("selectMultipleWithSameArgs")
            .sendQuery(listOf("myName", "myName2")).run {
                assertEquals("myName", rows[0].getString("firstName"))
                assertEquals("myName2", rows[0].getString("secondName"))
            }
    }

    @Test
    fun `call sendQuery with arguments on not same orders`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        Requester(connection, queriesDirectory = resources)
            .getQuery("selectMultipleDifferentArgs")
            .sendQuery("first" to "firstName", "second" to "secondName").run {
                assertEquals("firstName", rows[0].getString("firstName"))
                assertEquals("secondName", rows[0].getString("secondName"))
            }

        Requester(connection, queriesDirectory = resources)
            .getQuery("selectMultipleDifferentArgs")
            .sendQuery("second" to "secondName", "first" to "firstName").run {
                assertEquals("firstName", rows[0].getString("firstName"))
                assertEquals("secondName", rows[0].getString("secondName"))
            }

        Requester(connection, queriesDirectory = resources)
            .getQuery("selectMultipleDifferentArgs")
            .sendQuery("second" to "secondName", "first" to "firstName").run {
                assertEquals("firstName", rows[0].getString(0))
                assertEquals("secondName", rows[0].getString(1))
            }
    }

    @Test
    fun `call sendQuery with wrong number of arguments`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()

        assertThrows(QueryError::class.java) {
            Requester(connection, queriesDirectory = resources)
                .getQuery("selectMultipleDifferentArgs")
                .sendQuery("first" to "firstName")
        }.let {
            assertEquals(
                """
                Parameter "second" missing

                  > :first = firstName
                  > SELECT :first::text as "firstName", :second::text as "secondName";
                """.trimIndent(),
                it.message
            )
        }
    }

    @Test
    fun `call sendQuery with wrong number of arguments as list`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()

        assertThrows(QueryError::class.java) {
            Requester(connection, queriesDirectory = resources)
                .getQuery("selectMultipleDifferentArgs")
                .sendQuery(listOf("firstName"))
        }.let {
            assertEquals(
                """
                Parameter 1 missing
                
                  > firstName
                  > SELECT ?::text as "firstName", ?::text as "secondName";
                """.trimIndent(),
                it.message
            )
        }
    }

    @Test
    fun `call exec on function with pair as arguments`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val result = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function_void")
            .exec("name" to "test")

        assertEquals(1, result.rowsAffected)
    }

    @Test
    fun `call execute on function`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val obj: ObjTest? = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function")
            .execute(mapOf("name" to "myName"))

        assertNotNull(obj)
        assertEquals("myName", obj.name)
    }

    @Test
    fun `call execute on function with object and named argument`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val obj2 = ObjTest("original")
        val obj: ObjTest? = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function_object")
            .execute("resource" to obj2)

        assertNotNull(obj)
        assertEquals("changedName", obj.name)
        assertEquals("original", obj2.name)
    }

    @Test
    fun `call execute on function with object`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val obj2 = ObjTest("original")
        val obj: ObjTest? = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function_object")
            .execute(listOf(obj2))

        assertNotNull(obj)
        assertEquals("changedName", obj.name)
        assertEquals("original", obj2.name)
    }

    @Test
    fun `call execute on function with object and no arguments`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val obj: ObjTest? = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function")
            .execute()

        assertNotNull(obj)
        assertEquals("plop", obj.name)
    }

    @Test
    fun `call execute on query`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        val obj: ObjTest? = Requester(connection, queriesDirectory = resources)
            .getQuery("selectOneWithParameters")
            .execute(mapOf("name" to "myName"))

        assertNotNull(obj)
        assertEquals("myName", obj.name)
    }

    @Test
    fun `call select (multiple) on function with named argument`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val obj: List<ObjTest>? = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function_multiple")
            .execute(mapOf("name" to "myName"))

        assertNotNull(obj)
        assertNotNull(obj[0])
        assertEquals("myName", obj[0].name)
        assertEquals("myName", obj[0].name)
    }

    @Test
    fun `call select (multiple) on function with ordered arguments`() {
        val resources = this::class.java.getResource("/sql/function/Test")?.toURI()
        val obj: List<ObjTest>? = Requester(connection, functionsDirectory = resources)
            .getFunction("test_function_multiple")
            .execute(listOf("myName"))

        assertNotNull(obj)
        assertEquals("myName", obj[0].name)
    }

    @Test
    fun `call select multiple (named arguments)`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        Requester(connection, queriesDirectory = resources)
            .getQuery("selectMultiple").apply {
                execute<List<ObjTest>>(mapOf("name" to "ff")).let { result ->
                    assertNotNull(result)
                    assertEquals("ff", result[0].name)
                    assertEquals("ff-2", result[1].name)
                }
            }.apply {
                execute(object : TypeReference<List<ObjTest>>() {}, mapOf("name" to "ff")).let { result ->
                    assertNotNull(result)
                    assertEquals("ff", result[0].name)
                    assertEquals("ff-2", result[1].name)
                }
            }
    }

    @Test
    fun `call select multiple (named arguments as pair)`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        Requester(connection, queriesDirectory = resources)
            .getQuery("selectMultiple").apply {
                execute<List<ObjTest>>("name" to "ff").let { result ->
                    assertNotNull(result)
                    assertEquals("ff", result[0].name)
                    assertEquals("ff-2", result[1].name)
                }
            }.apply {
                execute(object : TypeReference<List<ObjTest>>() {}, "name" to "ff").let { result ->
                    assertNotNull(result)
                    assertEquals("ff", result[0].name)
                    assertEquals("ff-2", result[1].name)
                }
            }
    }

    @Test
    fun `call select multiple (ordered argument)`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        Requester(connection, queriesDirectory = resources)
            .getQuery("selectMultipleOrderedArgs").apply {
                execute<List<ObjTest>>(listOf("ff", "aa")).let { result ->
                    assertNotNull(result)
                    assertEquals("ff", result[0].name)
                    assertEquals("aa-2", result[1].name)
                }
            }.apply {
                execute(object : TypeReference<List<ObjTest>>() {}, listOf("ff", "aa")).let { result ->
                    assertNotNull(result)
                    assertEquals("ff", result[0].name)
                    assertEquals("aa-2", result[1].name)
                }
            }
    }

    @Test
    fun `call execute on query with extra parameter`() {
        val resources = this::class.java.getResource("/sql/query")?.toURI()
        Requester(connection, queriesDirectory = resources)
            .getQuery("selectOneWithParameters").apply {
                execute<ObjTest>(mapOf("name" to "myName")) {
                    assertNotNull(it)
                    assertEquals("myName", it.name)
                    assertEquals("plop", rows[0].getString("other"))
                }.run {
                    assertEquals("selectOneWithParameters", name)
                }
            }.apply {
                execute(typeReference = object : TypeReference<ObjTest>() {}, values = mapOf("name" to "myName")) {
                    assertNotNull(it)
                    assertEquals("myName", it.name)
                    assertEquals("plop", rows[0].getString("other"))
                }.run {
                    assertEquals("selectOneWithParameters", name)
                }
            }
    }
}
