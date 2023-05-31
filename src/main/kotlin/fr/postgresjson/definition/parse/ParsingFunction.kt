package fr.postgresjson.definition.parse

import fr.postgresjson.definition.Function
import fr.postgresjson.definition.Parameter
import fr.postgresjson.definition.Parameter.Direction
import fr.postgresjson.definition.Parameter.Direction.IN
import fr.postgresjson.definition.Parameter.Direction.INOUT
import fr.postgresjson.definition.Parameter.Direction.OUT
import fr.postgresjson.definition.ParameterType
import fr.postgresjson.definition.Resource.ParseException
import fr.postgresjson.definition.Returns
import fr.postgresjson.definition.Returns.Void
import java.nio.file.Path
import kotlin.text.RegexOption.IGNORE_CASE

internal fun parseFunction(script: String, source: Path? = null): Function {
    val name: String
    val parameters: List<Parameter>
    val returns: Returns
    ScriptPart(script)
        .getFunctionOrProcedure().trimSpace().nextScriptPart
        .getFunctionName().apply { name = value }.nextScriptPart
        .getParameters().apply { parameters = value }.nextScriptPart
        .getReturns().apply { returns = value }

    return Function(name, parameters, returns, script, source)
}

@Throws(FunctionNameMalformed::class)
internal fun ScriptPart.getFunctionName(): NextScript<String> {
    try {
        return getNextScript { status.isNotEscaped() && afterBeginBy("(", " ", "\n") }
    } catch (e: ParseException) {
        throw FunctionNameMalformed(this, e)
    }
}
internal class FunctionNameMalformed(val script: ScriptPart, cause: Throwable? = null) :
    ParseException("Function name is malformed", cause)

@Throws(FunctionNotFound::class)
internal fun ScriptPart.getFunctionOrProcedure(): NextScript<String> {
    val result = """create\s+(?:or\s+replace\s+)?(procedure|function)\s+"""
        .toRegex()
        .find(restOfScript)
        ?: throw FunctionNotFound(this)

    val rest = result.range.last
        .let { cursor -> restOfScript.drop(cursor + 1) }

    return NextScript(
        result.groups[1]!!.value,
        rest
    )
}

internal class FunctionNotFound(val script: ScriptPart) :
    ParseException("Function not found in script")

internal fun ScriptPart.getParameters(): NextScript<List<Parameter>> {
    val allParametersScript = this.getNextScript {
        currentChar == ')' && status.isNotEscaped()
    }
    val parameterList: List<Parameter> = allParametersScript
        .valueAsScriptPart()
        .removeParentheses()
        .split(",")
        .map { it.toParameter() }

    return NextScript(parameterList, allParametersScript.restOfScript)
}

private fun ScriptPart.toParameter(): Parameter {
    var script: ScriptPart = this.trimSpace()
    return Parameter(
        direction = script.getParameterMode().apply { script = nextScriptPart }.value,
        name = script.getParameterName().trimSpace().apply { script = nextScriptPart }.value.trim(),
        type = script.getParameterType().trimSpace().apply { script = nextScriptPart }.value,
        default = script.getParameterDefault().trimSpace().apply { script = nextScriptPart }.value,
    )
}
private fun ScriptPart.getParameterMode(): NextScript<Direction> {
    return when {
        restOfScript.startsWith("inout ", true) -> NextScript(INOUT, restOfScript.drop("inout ".length))
        restOfScript.startsWith("in ", true) -> NextScript(IN, restOfScript.drop("in ".length))
        restOfScript.startsWith("out ", true) -> NextScript(OUT, restOfScript.drop("out ".length))
        else -> NextScript(IN, restOfScript)
    }
}

@Throws(ParameterNameMalformed::class)
private fun ScriptPart.getParameterName(): NextScript<String> {
    try {
        return getNextScript { afterBeginBy(" ", "\n") }
    } catch (e: ParseException) {
        throw ParameterNameMalformed(this, e)
    }
}
private class ParameterNameMalformed(val script: ScriptPart, cause: Throwable) :
    ParseException("Parameter name is malformed", cause)

@Throws(ParameterTypeMalformed::class)
private fun ScriptPart.getParameterType(): NextScript<ParameterType> {
    val fullType = try {
        val endTextList = arrayOf(" default ", "=", ")")
        getNextScript { afterBeginBy(texts = endTextList) }
    } catch (e: ParseError) {
        throw ParameterTypeMalformed(this, e)
    }

    var rest: ScriptPart = fullType.valueAsScriptPart()

    val name = rest
        .getNextScript { afterBeginBy("(") }
        .apply { rest = nextScriptPart }
    val precision = rest
        .getNextInteger()
        .apply { rest = nextScriptPart }
    val scale = rest
        .getNextInteger()
        .apply { rest = nextScriptPart }

    return NextScript(
        ParameterType(
            name = name.value.trim(),
            precision = precision.value,
            scale = scale.value
        ),
        fullType.nextScriptPart.restOfScript
    )
}

internal class ParameterTypeMalformed(val script: ScriptPart, cause: Throwable) :
    ParseException("Parameter type is malformed", cause)

@Throws(ParameterDefaultMalformed::class)
private fun ScriptPart.getParameterDefault(): NextScript<String?> {
    return if (this.isEmpty() || this.restOfScript == ")") {
        NextScript(null, "")
    } else {
        """^(\s*=\s*|\s+default\s+)(.+)\s*$"""
            .toRegex(IGNORE_CASE)
            .find(restOfScript)
            .let { it ?: throw ParameterDefaultMalformed(this) }
            .let { it.groups[2]!!.value }
            .let { NextScript(it, "") }
    }
}

private class ParameterDefaultMalformed(val script: ScriptPart) :
    ParseException("Parameter default is malformed")

/**
 * TODO Finalize this
 */
internal fun ScriptPart.getReturns(): NextScript<Returns> {
    return NextScript(Void(), "")
}

class ParseError(message: String? = null, cause: Throwable? = null) :
    ParseException(message ?: "Parsing fail", cause)
