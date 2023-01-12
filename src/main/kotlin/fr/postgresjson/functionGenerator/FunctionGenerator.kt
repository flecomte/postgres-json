package fr.postgresjson.functionGenerator

import fr.postgresjson.definition.Function
import fr.postgresjson.definition.Parameter
import fr.postgresjson.definition.Parameter.Direction.IN
import fr.postgresjson.definition.Parameter.Direction.INOUT
import fr.postgresjson.definition.Parameter.Direction.OUT
import fr.postgresjson.utils.searchSqlFiles
import fr.postgresjson.utils.toCamelCase
import java.io.File
import java.net.URI
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FunctionGenerator(private val functionsDirectories: List<URI>) {
    constructor(functionsDirectories: URI): this(listOf(functionsDirectories))

    private val logger: Logger = LoggerFactory.getLogger("sqlFilesSearch")

    private fun List<Parameter>.toKotlinArgs(): String {
        return filter { it.direction == IN || it.direction == INOUT }
            .joinToString(", ") {
                val base = """${it.kotlinName}: ${it.kotlinType}"""
                val default = if (it.default == null) {
                    ""
                } else {
                    when (it.kotlinType) {
                        "String" -> """ = "${it.default.trim('\'')}""""
                        "Int" -> """ = ${it.default}"""
                        "Boolean" -> """ = ${it.default.lowercase()}"""
                        else -> ""
                    }
                }

                base+default
            }
    }
    private fun List<Parameter>.toMapOf(): String {
        return filter { it.direction == IN || it.direction == INOUT }
            .joinToString(", ", prefix = "mapOf(", postfix = ")") { """"${it.kotlinName}" to ${it.kotlinName}""" }
    }

    private val Parameter.kotlinType: String
        get() {
            return when (type.lowercase()) {
                "text" -> "String"
                "varchar" -> "String"
                "character varying" -> "String"
                "character" -> "String"
                "char" -> "String"
                "int" -> "Int"
                "boolean" -> "Boolean"
                "json" -> "S"
                "jsonb" -> "S"
                "any" -> "Any"
                "anyelement" -> "Any"
                "anyarray" -> "List<*>"
                else -> "String"
            }
        }

    private val Parameter.kotlinName: String
        get() {
            return name.toCamelCase().trimStart('_')
        }

    private val Function.kotlinName: String
        get() {
            return name.toCamelCase().trimStart('_')
        }

    fun generate(outputDirectory: URI) {
        File(outputDirectory.path).apply {
            logger.debug("Create Directory: $absolutePath")
            mkdirs()
        }

        this.functionsDirectories
            .flatMap { it.searchSqlFiles() }
            .filterIsInstance<Function>()
            .map { it.run {
                val args = parameters.toKotlinArgs()

                File("${outputDirectory.path}${kotlinName}.kt").apply {
                    logger.debug("Create kotlin file: $absolutePath")
                    val hasGenerics: Boolean = parameters.filter { it.direction != OUT }.any { it.kotlinType == "S" }
                    val genericsType = if (hasGenerics) ", S: Serializable" else ""

                    val hasReturn = parameters.any { it.direction != IN } || (it.returns != "" && it.returns != "void")
                    val returnTypeGenerics = if (hasReturn) "reified E: EntityI" else ""
                    val returnType = if (hasReturn) ": List<E>" else ""
                    val returnWord = if (hasReturn) "return " else ""
                    val select = if (hasReturn) "select<E>" else "exec"
                    val function = if (hasGenerics || hasReturn) """inline fun <$returnTypeGenerics$genericsType>""" else "fun"

                    val importEntityI = if (hasReturn) "import fr.postgresjson.entity.EntityI\n" else ""
                    val importSerializable = if (hasGenerics) "import fr.postgresjson.entity.Serializable\n" else ""
                    val importSelect = if (hasReturn) "import fr.postgresjson.connexion.select\n" else ""

                    writeText("""
                    |package fr.postgresjson.functionGenerator.generated
                    |
                    |import fr.postgresjson.connexion.Requester
                    |$importSelect$importSerializable$importEntityI
                    |$function Requester.$kotlinName($args)$returnType {
                    |    ${returnWord}getFunction("${it.name}")
                    |        .$select(${parameters.toMapOf()})
                    |}
                    """.trimMargin())
                }}
            }
    }
}