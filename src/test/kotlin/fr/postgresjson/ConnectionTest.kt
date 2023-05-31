package fr.postgresjson

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.connexion.DataNotFoundException
import fr.postgresjson.connexion.SqlSerializable
import fr.postgresjson.connexion.execute
import fr.postgresjson.serializer.deserialize
import fr.postgresjson.serializer.toTypeReference
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import java.util.UUID
import kotlin.reflect.full.hasAnnotation
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.assertThrows

class ConnectionTest: StringSpec({
    val connection = TestConnection()

    @SqlSerializable
    class ObjTest(val name: String, val id: UUID = UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00"))

    @SqlSerializable
    class ObjTest2(val id: UUID, val title: String, var test: ObjTest?)

    @SqlSerializable
    class ObjTest3(val id: UUID, val first: String, var second: String, var third: Int)

    @SqlSerializable
    class ParameterObject(var third: String)

    @SqlSerializable
    class ObjTestWithParameterObject(val id: UUID, var first: ParameterObject, var second: ParameterObject)
    class ObjTest4

    "serializable" {
        assertTrue(ObjTest("plop")::class.hasAnnotation<SqlSerializable>())
        assertFalse(ObjTest4()::class.hasAnnotation<SqlSerializable>())
    }

    "getObject" {
        val obj: ObjTest? = connection.rollbackAfter {
            sendQuery(
                """
                create table test(
                    id UUID primary key,
                    name text
                );
                INSERT INTO test (id, name) VALUES ('1e5f5d41-6d14-4007-897b-0ed2616bec96', 'one');
                INSERT INTO test (id, name) VALUES ('26fa76cf-7688-4a1d-b611-e3060b38bf58', 'two');
                """.trimIndent()
            )

            execute("select to_json(a) from test a limit 1")
        }

        assertNotNull(obj)
        assertEquals(UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"), obj.id)
    }

    "getExistingObject" {
        val objs: List<ObjTest2>? = connection.rollbackAfter {
            sendQuery(
                """
                create table test(
                    id uuid primary key,
                    name text
                );
                create table test2(
                    id uuid primary key,
                    title text,
                    test_id uuid
                );
                INSERT INTO test VALUES ('1e5f5d41-6d14-4007-897b-0ed2616bec96', 'one');
                INSERT INTO test2 VALUES ('a0214677-7332-4eec-8e9b-af0658ea72a6', 'two', '1e5f5d41-6d14-4007-897b-0ed2616bec96');
                INSERT INTO test2 VALUES ('8545577e-2785-421f-bb7e-1ec3faa1d79a', 'three', null);
                """.trimIndent()
            )

            execute<List<ObjTest2>>(
                """
                select json_agg(j)
                from (
                    select
                        t.id, 
                        t.title,
                        t2 as test
                    from test2 t
                    join test t2 ON t.test_id = t2.id
                ) j;
                """.trimIndent()
            )
        }

        objs.shouldNotBeNull()
        objs.size `should be equal to` 1
        objs.first().id `should be equal to` UUID.fromString("a0214677-7332-4eec-8e9b-af0658ea72a6")
        objs.first().title `should be equal to` "two"
        objs.first().test!!.id `should be equal to` UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96")
        objs.first().test!!.name `should be equal to` "one"
    }

    "test call request with args" {
        val result: ObjTest? = connection.execute(
            "select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', ?::text)",
            listOf("myName")
        )
        result.shouldNotBeNull()
        result.name `should be equal to` "myName"
    }

    "test call request without args" {
        val result: ObjTest? = connection.execute(
            "select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', 'myName')",
            object: TypeReference<ObjTest>() {}
        ) {
            assertEquals("myName", this.deserialize<ObjTest>()?.name)
        }
        result.shouldNotBeNull()
        result.name `should be equal to` "myName"
    }

    "test call request return null" {
        val result: ObjTest? = connection.execute("select null;", object: TypeReference<ObjTest>() {})
        result.shouldBeNull()
    }

    "test call request return nothing" {
        val e = connection.rollbackAfter {
            sendQuery(
                """
                create table test(
                    id UUID primary key,
                    name text
                );
                """.trimIndent()
            )

            assertThrows<DataNotFoundException> {
                execute("select * from test where false;", object: TypeReference<ObjTest>() {})
            }
        }

        e.shouldNotBeNull()
        e.message `should be equal to` "No data return for the query"
        e.queryExecuted `should be equal to` "select * from test where false;"
    }

    "callRequestWithArgsEntity" {
        val o = ObjTest("myName", id = UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00"))
        val obj: ObjTest? = connection.execute(
            "select json_build_object('id', id, 'name', name) FROM json_to_record(?::json) as o(id uuid, name text);",
            listOf(o)
        )
        obj.shouldNotBeNull()
        obj.id `should be equal to` UUID.fromString("2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00")
        obj.name `should be equal to` "myName"
    }

    "test update Entity" {
        val obj = ObjTest("before", id = UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
        val objUpdated: ObjTest? = connection.execute(
            "select ?::jsonb || jsonb_build_object('name', 'after');",
            obj.toTypeReference(), listOf(obj)
        )
        objUpdated.shouldNotBeNull()
        objUpdated.id `should be equal to` UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96")
        objUpdated.name `should be equal to` "after"
    }

    "test update Entity with vararg" {
        val obj = ObjTest("before", id = UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"))
        val objUpdated: ObjTest? = connection.execute(
            "select :obj::jsonb || jsonb_build_object('name', 'after');",
            obj.toTypeReference(),
            "obj" to obj
        )
        assertNotNull(objUpdated)
        assertEquals(UUID.fromString("1e5f5d41-6d14-4007-897b-0ed2616bec96"), objUpdated.id)
        assertEquals("after", objUpdated.name)
    }

    "callExec" {
        val o = ObjTest("myName")
        val result = connection.exec(
            "select json_build_object('id', '2c0243ed-ff4d-4b9f-a52b-e38c71b0ed00', 'name', ?::json->>'name')",
            listOf(o)
        )
        assertEquals(1, result.rowsAffected)
    }

    "select one with named parameters" {
        val result: ObjTest3? = connection.execute(
            """
            SELECT json_build_object(
                'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                'first', :first::text, 
                'second', :second::text, 
                'third', :third::int
            )
            """.trimIndent(),
            mapOf(
                "first" to "ff",
                "second" to "sec",
                "third" to 123
            )
        )
        assertNotNull(result)
        assertEquals("ff", result.first)
        assertEquals("sec", result.second)
        assertEquals(123, result.third)
    }

    "select one with named parameters object" {
        val result: ObjTestWithParameterObject? = connection.execute(
            """
            SELECT json_build_object(
                'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                'first', :first::json, 
                'second', :second::json
            )
            """.trimIndent(),
            mapOf(
                "first" to ParameterObject("one"),
                "second" to ParameterObject("two")
            )
        )
        assertNotNull(result)
        assertEquals("bf0e5605-3a8f-4db9-8b98-c8e0691dd576", result.id.toString())
        assertEquals("one", result.first.third)
        assertEquals("two", result.second.third)
    }

    "select with named parameters" {
        val result: List<ObjTest3>? = connection.execute(
            """
            SELECT json_build_array(
                json_build_object(
                    'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                    'first', :first::text, 
                    'second', :second::text, 
                    'third', :third::int
                ),
                json_build_object(
                    'id', 'ce9ae3c9-dc0e-4561-a168-811b996d913e', 
                    'first', :first::text, 
                    'second', :second::text, 
                    'third', :third::int
                )
            )
            """.trimIndent(),
            mapOf(
                "first" to "ff",
                "third" to 123,
                "second" to "sec"
            )
        )
        assertNotNull(result)
        assertEquals("bf0e5605-3a8f-4db9-8b98-c8e0691dd576", result[0].id.toString())
        assertEquals("ff", result[0].first)
        assertEquals("sec", result[0].second)
        assertEquals(123, result[0].third)
    }

    "select with named parameters as vararg of Pair" {
        val result: List<ObjTest3>? = connection.execute(
            """
            SELECT json_build_array(
                json_build_object('id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 'first', :first::text, 'second', :second::text, 'third', :third::int),
                json_build_object('id', '0c9d55d2-f69a-4750-a278-fac821774276', 'first', :first::text, 'second', :second::text, 'third', :third::int)
            )
            """.trimIndent(),
            "first" to "ff",
            "third" to 123,
            "second" to "sec"
        )
        assertNotNull(result)
        assertEquals("bf0e5605-3a8f-4db9-8b98-c8e0691dd576", result[0].id.toString())
        assertEquals("ff", result[0].first)
        assertEquals("sec", result[0].second)
        assertEquals(123, result[0].third)
    }

    "execute with extra parameters" {
        val params: Map<String, Any?> = mapOf(
            "first" to "ff",
            "third" to 123,
            "second" to "sec"
        )
        val result: ObjTest3? = connection.execute(
            """
            SELECT json_build_object(
                'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                'first', :first::text, 
                'second', :second::text, 
                'third', :third::int
            ), 'plop'::text as other
            """.trimIndent(),
            params
        ) {
            assertNotNull(it)
            assertEquals("bf0e5605-3a8f-4db9-8b98-c8e0691dd576", it.id.toString())
            assertEquals("ff", it.first)
            assertEquals("plop", rows[0].getString("other"))
        }
        assertNotNull(result)
        assertEquals("ff", result.first)
        assertEquals("sec", result.second)
        assertEquals(123, result.third)
    }

    "test exec without parameters" {
        connection.exec("select 42, 'hello';").run {
            assertEquals(42, rows[0].getInt(0))
            assertEquals("hello", rows[0].getString(1))
        }
    }

    "test exec with one object as parameter" {
        val obj = ObjTest("myName", UUID.fromString("c606e216-53b3-43c8-a900-e727cb4a017c"))
        connection.exec("select ?::jsonb->>'name'", obj).run {
            assertEquals("myName", rows[0].getString(0))
        }
    }

    "select one in transaction" {
        connection.inTransaction {
            execute<ObjTestWithParameterObject>(
                """
                SELECT json_build_object(
                    'id', 'bf0e5605-3a8f-4db9-8b98-c8e0691dd576', 
                    'first', :first::json, 
                    'second', :second::json
                    )
                """.trimIndent(),
                mapOf(
                    "first" to ParameterObject("one"),
                    "second" to ParameterObject("two")
                )
            ).let { result ->
                assertNotNull(result)
                assertEquals("one", result.first.third)
                assertEquals("two", result.second.third)
            }
        }
    }
})
