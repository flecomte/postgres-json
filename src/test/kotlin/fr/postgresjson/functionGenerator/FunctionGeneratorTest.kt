package fr.postgresjson.functionGenerator

import fr.postgresjson.definition.Function
import io.kotest.core.Tag
import io.kotest.core.spec.style.StringSpec
import org.amshove.kluent.`should be equal to`

class FunctionGeneratorTest : StringSpec({
    tags(Tag("Generator"))
    val functionDirectory = this::class.java.getResource("/sql/function/Test")!!.toURI()
    val generator = FunctionGenerator(functionDirectory)

    "generate function with input object and output object" {
        val functionSql = """
            |create or replace function test_function_object (inout resource json)
            |language plpgsql
            |as
            |$$
            |begin
            |    resource = json_build_object('id', '1e5f5d41-6d14-4007-897b-0ed2616bec96', 'name', 'changedName');
            |end;
            |$$
        """.trimMargin()

        val expectedGenerated = """
            |package fr.postgresjson.functionGenerator.generated
            |
            |import com.fasterxml.jackson.core.type.TypeReference
            |import fr.postgresjson.connexion.Requester
            |import fr.postgresjson.entity.Serializable
            |
            |inline fun <reified E: Any?, S: Serializable> Requester.testFunctionObject(resource: S): E {
            |    return getFunction("test_function_object")
            |        .selectAny<E>(object : TypeReference<E>() {}, mapOf("resource" to resource))
            |}
        """.trimMargin()

        generator.generate(Function(functionSql)) `should be equal to` expectedGenerated
    }

    "generate function with return void" {
        val functionSql = """
            |create or replace function test_function_void (name text default 'plop') returns void
            |language plpgsql
            |as
            |$$
            |begin
            |    perform 1;
            |end;
            |$$;
        """.trimMargin()

        val expectedGenerated = """
            |package fr.postgresjson.functionGenerator.generated
            |
            |import fr.postgresjson.connexion.Requester
            |
            |fun Requester.testFunctionVoid(name: String = "plop"): Unit {
            |    getFunction("test_function_void")
            |        .exec(mapOf("name" to name))
            |}
        """.trimMargin()

        generator.generate(Function(functionSql)) `should be equal to` expectedGenerated
    }

    "generate function with multiple args and defaults" {
        val functionSql = """
            |create or replace function test_function_multiple (name text default 'plop', in hi text default 'hello', out result json)
            |language plpgsql
            |as
            |$$
            |begin
            |    result = json_build_array(
            |        json_build_object('id', '457daad5-4f1b-4eb7-80ec-6882adb8cc7d', 'name', name),
            |        json_build_object('id', '8d20abb0-7f77-4b6c-9991-44acd3c88faa', 'name', hi)
            |    );
            |end;
            |$$
        """.trimMargin()

        val expectedGenerated = """
            |package fr.postgresjson.functionGenerator.generated
            |
            |import com.fasterxml.jackson.core.type.TypeReference
            |import fr.postgresjson.connexion.Requester
            |
            |inline fun <reified E: Any?> Requester.testFunctionMultiple(name: String = "plop", hi: String = "hello"): E {
            |    return getFunction("test_function_multiple")
            |        .selectAny<E>(object : TypeReference<E>() {}, mapOf("name" to name, "hi" to hi))
            |}
        """.trimMargin()

        generator.generate(Function(functionSql)) `should be equal to` expectedGenerated
    }
})