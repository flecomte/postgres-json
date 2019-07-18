package fr.postgresjson.definition

import java.io.File


open class Function(
    override val script: String
): Resource, ParametersInterface {
    val returns: String?
    override val name: String
    override val parameters: List<Parameter>
    override var source: File? = null

    init {
        val functionRegex =
            """create (or replace )?(procedure|function) *(?<name>[^(\s]+)\s*\((?<params>(\s*((IN|OUT|INOUT|VARIADIC)?\s+)?([^\s,)]+\s+)?([^\s,)]+)(\s+(?:default\s|=)\s*[^\s,)]+)?\s*(,|(?=\))))*)\) *(?<return>RETURNS *[^ ]+)?"""
                .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

        val paramsRegex =
            """\s*(?<param>((?<direction>IN|OUT|INOUT|VARIADIC)?\s+)?("?(?<name>[^\s,")]+)"?\s+)?(?<type>[^\s,)]+)(\s+(?<default>default\s|=)\s*[^\s,)]+)?)\s*(,|$)"""
                .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

        val queryMatch = functionRegex.find(script)
        if (queryMatch !== null) {
            val functionName = queryMatch.groups.get("name")?.value?.trim()
            val functionParameters = queryMatch.groups["params"]?.value?.trim()
            this.returns = queryMatch.groups["return"]?.value?.trim()

            /* Create parameters definition */
            val parameters = if (functionParameters !== null) {
                val matchesParams = paramsRegex.findAll(functionParameters)
                matchesParams.map { paramsMatch ->
                    Parameter(
                        paramsMatch.groups["name"]!!.value.trim(),
                        paramsMatch.groups["type"]!!.value.trim(),
                        paramsMatch.groups["direction"]?.value?.trim(),
                        paramsMatch.groups["default"]?.value?.trim()
                    )
                }.toList()
            } else {
                listOf()
            }
            this.name = functionName!!
            this.parameters = parameters
        } else {
            throw FunctionNotFound()
        }
    }

    abstract class ParseException(message: String, cause: Throwable? = null): Exception(message, cause)
    class FunctionNotFound(cause: Throwable? = null): ParseException("Function not found in script", cause)

    fun getDefinition(): String {
        return "$name (" + parameters.joinToString(", ") + ") $returns"
    }

    fun getParametersIndexedByName(): Map<String, Parameter> {
        return parameters.map {
            it.name to it
        }.toMap()
    }

    infix fun `has same definition`(other: Function): Boolean {
        return other.getDefinition() == this.getDefinition()
    }

    infix fun `is same`(other: Function): Boolean {
        return other.script == this.script
    }

    companion object {
        fun build(source: File): List<Function> {
            return source.readText()
                .split(
                    "CREATE +(OR REPLACE +)?(PROCEDURE|FUNCTION)".toRegex(
                        setOf(
                            RegexOption.IGNORE_CASE,
                            RegexOption.MULTILINE
                        )
                    )
                )
                .map {
                    Function("CREATE OR REPLACE FUNCTION $it")
                }
        }
    }
}