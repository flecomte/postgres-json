package fr.postgresjson.functionGenerator

import com.github.jasync.sql.db.util.length
import fr.postgresjson.definition.Function
import fr.postgresjson.definition.Function.Returns
import fr.postgresjson.definition.Parameter
import fr.postgresjson.definition.Parameter.Direction.IN
import fr.postgresjson.definition.Parameter.Direction.INOUT
import fr.postgresjson.definition.Parameter.Direction.OUT
import fr.postgresjson.utils.searchSqlFiles
import fr.postgresjson.utils.toCamelCase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI

class FunctionGenerator(private val functionsDirectories: List<URI>) {
    constructor(functionsDirectories: URI) : this(listOf(functionsDirectories))

    private val logger: Logger = LoggerFactory.getLogger("sqlFilesSearch")

    private fun List<Parameter>.toKotlinArgs(): String {
        return filter { it.direction == IN || it.direction == INOUT }
            .mapIndexed { index, parameter -> index to parameter }
            .joinToString(", ") { (idx, param) ->
                val base = """${param.kotlinName ?: "arg$idx"}: ${param.kotlinType}"""
                val default = if (param.default == null) {
                    ""
                } else {
                    when (param.kotlinType) {
                        "String" -> """ = "${param.default.trim('\'')}""""
                        "Int" -> """ = ${param.default}"""
                        "Boolean" -> """ = ${param.default.lowercase()}"""
                        else -> ""
                    }
                }

                base + default
            }
    }

    private fun List<Parameter>.toMapOf(): String {
        return filter { it.direction == IN || it.direction == INOUT }
            .joinToString(", ", prefix = "mapOf(", postfix = ")") { """"${it.kotlinName}" to ${it.kotlinName}""" }
    }

    private val Parameter.kotlinType: String
        get() {
            return when (type.name.lowercase()) {
                "text" -> "String"
                "varchar" -> "String"
                "character varying" -> "String"
                "character" -> "String"
                "char" -> "String"
                "int" -> "Int"
                "smallint" -> "Int"
                "integer" -> "Int"
                "bigint" -> "Int"
                "decimal" -> "Float"
                "real" -> "Float"
                "double precision" -> "Float"
                "float" -> "Float"
                "numeric" -> "Number"
                "boolean" -> "Boolean"
                "json" -> "S"
                "jsonb" -> "S"
                "any" -> "Any"
                "anyelement" -> "Any"
                "anyarray" -> "List<*>"
                else -> "String"
            }
        }

    private val Parameter.kotlinName: String?
        get() {
            return name?.toCamelCase()?.trimStart('_')
        }

    private val Function.kotlinName: String
        get() {
            return name.toCamelCase().trimStart('_')
        }

    private val functions: List<Function>
        get() = functionsDirectories
            .flatMap { it.searchSqlFiles() }
            .filterIsInstance<Function>()

    fun generate(outputDirectory: URI) {
        File(outputDirectory.path).apply {
            logger.debug("Create Directory: $absolutePath")
            mkdirs()
        }

        functions
            .map { function ->
                File("${outputDirectory.path}${function.kotlinName}.kt").apply {
                    writeText(generate(function))
                }
            }
    }

    fun generate(functionName: String): String {
        return functions
            .first { it.name == functionName }
            .let { generate(it) }
    }

    fun generate(function: Function): String = function.run {
        val args = parameters.toKotlinArgs()

        val hasInputArgs: Boolean = parameters.filter { it.direction != OUT }.any { it.kotlinType == "S" }
        val hasReturn: Boolean = parameters.any { it.direction != IN } || (returns !is Returns.Void)

        val generics = mutableListOf<String>()
        if (hasReturn) generics.add("reified E: Any")
        if (hasInputArgs) generics.add("S: Any?")

        val functionDecl = if (generics.isNotEmpty()) "inline fun <${generics.joinToString(", ")}>" else "fun"

        if (hasReturn) {
            return """
            |package fr.postgresjson.functionGenerator.generated
            |
            |import com.fasterxml.jackson.core.type.TypeReference
            |import fr.postgresjson.connexion.Requester
            |
            |$functionDecl Requester.$kotlinName($args): E? {
            |    return getFunction("$name")
            |        .execute<E>(object : TypeReference<E>() {}, ${parameters.toMapOf()})
            |}
        """.trimMargin()
        } else {
            return """
            |package fr.postgresjson.functionGenerator.generated
            |
            |import fr.postgresjson.connexion.Requester
            |
            |$functionDecl Requester.$kotlinName($args): Unit {
            |    getFunction("$name")
            |        .exec(${parameters.toMapOf()})
            |}
        """.trimMargin()
        }
    }
}
