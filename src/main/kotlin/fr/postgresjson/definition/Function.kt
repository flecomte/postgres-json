package fr.postgresjson.definition

import java.nio.file.Path

class Function(
    override val script: String,
    override val source: Path? = null
) : Resource, ParametersInterface {
    val returns: String
    override val name: String
    override val parameters: List<Parameter>

    init {
        val functionRegex =
            """create (or replace )?(procedure|function) *(?<name>[^(\s]+)\s*\((?<params>(\s*((IN|OUT|INOUT|VARIADIC)?\s+)?([^\s,)]+\s+)?([^\s,)]+)(\s+(?:default\s|=)\s*[^\s,)]+)?\s*(,|(?=\))))*)\) *(?<return>RETURNS *[^ \n]+)?"""
                .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

        val paramsRegex =
            """\s*(?<param>((?<direction>IN|OUT|INOUT|VARIADIC)?\s+)?("?(?<name>[^\s,")]+)"?\s+)?(?<type>[^\s,)]+)(\s+(?<default>default\s|=)\s*[^\s,)]+)?)\s*(,|$)"""
                .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

        val queryMatch = functionRegex.find(script)
        if (queryMatch !== null) {
            val functionName = queryMatch.groups["name"]?.value?.trim() ?: error("Function name not found")
            val functionParameters = queryMatch.groups["params"]?.value?.trim()
            this.returns = queryMatch.groups["return"]?.value?.trim() ?: ""

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
            this.name = functionName
            this.parameters = parameters
        } else {
            throw FunctionNotFound()
        }
    }

    class FunctionNotFound(cause: Throwable? = null) : Resource.ParseException("Function not found in script", cause)

    fun getDefinition(): String {
        return parameters
            .filter { it.direction == Parameter.Direction.IN }
            .joinToString(", ") { "${it.name} ${it.type}" }
            .let { "$name ($it)" }
    }

    fun getParametersIndexedByName(): Map<String, Parameter> {
        return parameters.associateBy { it.name }
    }

    infix fun `has same definition`(other: Function): Boolean {
        return other.getDefinition() == this.getDefinition()
    }

    infix fun `is different from`(other: Function): Boolean {
        return other.script != this.script
    }
}
