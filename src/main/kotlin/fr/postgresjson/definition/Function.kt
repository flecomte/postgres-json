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
            """create (or replace )?(procedure|function) *(?<fname>[^(\s]+)\s*\(\s*(?<params>\s*([^()]+(\([^)]+\))*)*)\s*\)(RETURNS *(?<return>[^ \n]+))?"""
                .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

        val paramsRegex =
            """\s*(?<param>((?<direction>IN|OUT|INOUT|VARIADIC)?\s+)?("?(?<pname>[^\s,")]+)"?\s+)?(?<type>((?!default)[a-z0-9]+\s?)+(\((?<precision>[0-9]+)(, (?<scale>[0-9]+))?\))?)(\s+(default\s|=)\s*(?<default>('[^']+?'|[0-9]+|true|false))(?<defaultType>\s*::\s*[a-z0-9]+(\([0-9]+(\s?,\s?[0-9]+\s?)?\))?)?)?)\s*(,|${'$'})"""
                .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

        val queryMatch = functionRegex.find(script)
        if (queryMatch !== null) {
            val functionName = queryMatch.groups["fname"]?.value?.trim() ?: error("Function name not found")
            val functionParameters = queryMatch.groups["params"]?.value?.trim()
            this.returns = queryMatch.groups["return"]?.value?.trim() ?: ""

            /* Create parameters definition */
            val parameters = if (functionParameters !== null) {
                paramsRegex
                    .findAll(functionParameters)
                    .mapIndexed { index, paramsMatch ->
                        Parameter(
                            paramsMatch.groups["pname"]?.value?.trim() ?: """arg${index+1}""",
                            paramsMatch.groups["type"]?.value?.trim() ?: throw ArgumentNotFound(),
                            paramsMatch.groups["direction"]?.value?.trim(),
                            paramsMatch.groups["default"]?.value?.trim(),
                            paramsMatch.groups["precision"]?.value?.trim()?.toInt(),
                            paramsMatch.groups["scale"]?.value?.trim()?.toInt()
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
    class ArgumentNotFound(cause: Throwable? = null) : Resource.ParseException("Argument not found in script", cause)

    fun getDefinition(): String {
        return parameters
            .filter { it.direction == Parameter.Direction.IN }
            .joinToString(", ") { it.type }
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
