package fr.postgresjson.definition

import java.io.File


open class Function (
    override val name: String,
    override val script: String,
    override val parameters: List<Parameter>
) : Resource, ParametersInterface {
    override var source: File? = null

    companion object {
        fun build(source: File): List<Function> {
            return build(source.readText())
        }

        fun build(functionContent: String): List<Function> {
            val functionRegex = """create .*(procedure|function) *(?<name>[^(\s]+)\s*\((?<params>(\s*((IN|OUT|INOUT|VARIADIC)?\s+)?([^\s,)]+\s+)?([^\s,)]+)(\s+(?:default\s|=)\s*[^\s,)]+)?\s*(,|(?=\))))*)\) *(?<return>RETURNS *[^ ]+)?"""
                .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

            val paramsRegex = """\s*(?<param>((?<direction>IN|OUT|INOUT|VARIADIC)?\s+)?(?<name>[^\s,)]+\s+)?(?<type>[^\s,)]+)(\s+(?<default>default\s|=)\s*[^\s,)]+)?)\s*(,|$)"""
                .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

            return functionRegex.findAll(functionContent).map { queryMatch ->
                val functionName = queryMatch.groups["name"]?.value?.trim()
                val functionParameters = queryMatch.groups["params"]?.value?.trim()
                val returns = queryMatch.groups["return"]?.value?.trim()

                /* Create parameters definition */
                val parameters = if (functionParameters !== null) {
                    val matchesParams = paramsRegex.findAll(functionParameters)
                    matchesParams.map { paramsMatch ->
                        Parameter(
                            paramsMatch.groups["name"]!!.value.trim(),
                            paramsMatch.groups["type"]!!.value.trim(),
                            paramsMatch.groups["direction"]?.value?.trim(),
                            paramsMatch.groups["default"]?.value?.trim())
                    }.toList()
                } else {
                    listOf()
                }

                Function(functionName!!, functionContent, parameters)
            }.toList()
        }
    }
}