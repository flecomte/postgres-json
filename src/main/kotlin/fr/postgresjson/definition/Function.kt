package fr.postgresjson.definition

import fr.postgresjson.definition.Parameter.Direction
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.text.RegexOption.IGNORE_CASE

class Function(
    override val script: String,
    override val source: Path? = null,
): Resource, ParametersInterface {
    /**
     * TEXT, INT, TEXT[], CUSTOM_TYPE => String, Int, List<String>, Any;
     * TABLE(id INT, name TEXT)       => Object { id: Int; name: String };
     * SETOF TEXT                     => List<String>;
     * MY_TABLE.id%TYPE               => Any;
     * VOID                           => null;
     */
    val returns: Returns = Returns.Void()
    override val name: String
    override val parameters: List<Parameter>


    @JvmInline
    private value class ScriptPart(val restOfScript: String) {
        fun copy(block: (String) -> String): ScriptPart {
            return ScriptPart(block(restOfScript))
        }

        fun removeParentheses(): ScriptPart {
            return if (restOfScript.take(1) == "(" && restOfScript.takeLast(1) == ")") {
                this.copy {
                    it.drop(1).dropLast(1)
                }
            } else {
                this
            }
        }

        fun isEmpty() = restOfScript.isEmpty()
    }

    private fun emptyScriptPart(): ScriptPart = ScriptPart("")

    private class NextScript<T>(val value: T, val restOfScript: String) {
        val nextScriptPart: ScriptPart = ScriptPart(restOfScript)
        fun isLast() = restOfScript == ""
        fun isEmptyValue() = value == "" || value == null
    }

    /**
     * Return the value as ScriptPart
     */
    private fun NextScript<String>.valueAsScriptPart(): ScriptPart = ScriptPart(value)

    init {
        ScriptPart(script)
            .getFunctionOrProcedure().trimSpace().nextScriptPart
            .getFunctionName().apply { name = value }.nextScriptPart
            .getArguments().apply { parameters = value }.nextScriptPart
//            .getReturns().hook { returns = value }

    }

    private fun ScriptPart.getFunctionOrProcedure(): NextScript<String> {
        val result = """create\s+(?:or\s+replace\s+)?(procedure|function)\s+"""
            .toRegex()
            .find(restOfScript)
            ?: throw FunctionNotFound()

        val rest = result.range.last
            .let { cursor -> restOfScript.drop(cursor + 1) }

        return NextScript(
            result.groups[1]!!.value,
            rest
        )
    }

    private fun ScriptPart.getFunctionName(): NextScript<String> {
        try {
            return getNextScript { status.isNotEscaped() && afterBeginBy("(", " ", "\n") }
        } catch (e: NameMalformed) {
            throw FunctionNameMalformed(null, e)
        }
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun ScriptPart.change(block: String.() -> String): ScriptPart {
        contract {
            callsInPlace(block, EXACTLY_ONCE)
        }
        return ScriptPart(restOfScript.run(block))
    }

    data class Status(
        var doubleQuoted: Boolean = false, // "
        var simpleQuoted: Boolean = false, // '
        var parentheses: Int = 0, // ()
        var brackets: Int = 0, // []
        var braces: Int = 0, // {}
    ) {
        fun isQuoted(): Boolean = doubleQuoted || simpleQuoted
        fun isNotQuoted(): Boolean = !isQuoted()
        fun isNotEscaped(): Boolean = isNotQuoted() && parentheses == 0 && brackets == 0 && braces == 0
    }

    data class Context(
        val index: Int,
        val currentChar: Char,
        val status: Status,
        val script: String,
    ) {
        fun afterBeginBy(vararg texts: String): Boolean = texts.any {
            script.substring(index + 1).take(it.length) == it
        }

        val nextChar: Char? get() = script.substring(index + 1).getOrNull(0)
    }

    /**
     * Get next part of script.
     * You can define a list of characters that end the part of script. Like `(` or space.
     */
    private fun ScriptPart.getNextScript(isEnd: Context.() -> Boolean = { false }): NextScript<String> {
        val status = Status()

        fun String.unescape(): String {
            val first = take(1)
            val last = takeLast(1)
            return if (first == last && first in listOf("\"", "'")) {
                drop(1).dropLast(1).replace("$first$first", first)
            } else {
                this
            }
        }

        for ((index, c) in restOfScript.withIndex()) {
            val nextChar = restOfScript.getOrNull(index + 1)
            val prevChar = restOfScript.getOrNull(index - 1)
            if (c == '"' && (nextChar != '"' && prevChar != '"')) {
                status.doubleQuoted = !status.doubleQuoted
            } else if (c == '\'' && (nextChar != '\'' && prevChar != '\'')) {
                status.simpleQuoted = !status.simpleQuoted
            } else if (c == '(' && status.isNotQuoted()) {
                status.parentheses++
            } else if (c == ')' && status.isNotQuoted()) {
                status.parentheses--
            } else if (c == '[' && status.isNotQuoted()) {
                status.brackets++
            } else if (c == ']' && status.isNotQuoted()) {
                status.brackets--
            } else if (c == '{' && status.isNotQuoted()) {
                status.braces++
            } else if (c == '}' && status.isNotQuoted()) {
                status.braces--
            }

            if (isEnd(Context(index, c, status.copy(), restOfScript))) {
                return NextScript(restOfScript.take(index + 1).unescape(), restOfScript.drop(index + 1))
            }
        }
        if (status.isNotEscaped()) {
            return NextScript(restOfScript.unescape().trim(), "").trimSpace()
        }
        throw ParseError()
    }

    private fun ScriptPart.split(delimiter: String): List<ScriptPart> {
        val parts: MutableList<ScriptPart> = mutableListOf()
        var rest: ScriptPart = this
        do {
            rest = rest.trimSpace()
                .getNextScript { status.isNotEscaped() && currentChar.toString() == delimiter }
                .trimSpace()
                .also { parts.add(it.valueAsScriptPart().trimSpace().trimEnd(',')) }
                .nextScriptPart
        } while (!rest.isEmpty())

        return parts
    }

    private fun ScriptPart.getNextInteger(): NextScript<Int?> {
        val trimmed = restOfScript.trimStart { !it.isDigit() }
        val digits = trimmed.takeWhile { it.isDigit() }
        val restOfScript = trimmed.trimStart { it.isDigit() }
        return NextScript(digits.toIntOrNull(), restOfScript).trimSpace()
    }

    private fun ScriptPart.getArguments(): NextScript<List<Parameter>> {
        val allArgumentsScript = this.getNextScript {
            currentChar == ')' && status.isNotEscaped()
        }
        val arguments: List<Parameter> = allArgumentsScript
            .valueAsScriptPart()
            .removeParentheses()
            .split(",")
            .map { it.toArgument() }

        return NextScript(arguments.toList(), allArgumentsScript.restOfScript)
    }

    private fun ScriptPart.trimSpace(): ScriptPart {
        for ((n, char) in restOfScript.withIndex()) {
            if (char !in listOf(' ', '\n', '\t')) {
                return ScriptPart(
                    restOfScript.drop(n)
                )
            }
        }
        return ScriptPart(restOfScript)
    }

    private fun <T> NextScript<T>.trimSpace(): NextScript<T> {
        val spaces = charArrayOf(' ', '\n', '\t')
        return trim(chars = spaces)
    }

    private fun ScriptPart.trimEnd(vararg chars: Char): ScriptPart {
        return this.change { dropLastWhile { it in chars } }
    }

    private fun <T> NextScript<T>.trim(vararg chars: Char): NextScript<T> {
        return NextScript(value, restOfScript.apply { dropWhile { it in chars } })
    }

    private fun <T> NextScript<T>.changeValue(block: (T) -> T): NextScript<T> {
        return NextScript(block(value), restOfScript)
    }

    private fun <T> NextScript<T>.changeScript(block: (String) -> String): NextScript<T> {
        return NextScript(value, block(restOfScript))
    }

    private fun <T> NextScript<T>.dropOneOf(vararg endTextList: String): NextScript<T> {
        return changeScript { script ->
            endTextList
                .filter { script.startsWith(it) }
                .let { script.drop(it.size) }
        }
    }

    private fun ScriptPart.toArgument(): Parameter {
        var script: ScriptPart = this.trimSpace()
        return Parameter(
            direction = script.getArgMode().apply { script = nextScriptPart }.value,
            name = script.getArgName().trimSpace().apply { script = nextScriptPart }.value.trim(),
            type = script.getArgType().trimSpace().apply { script = nextScriptPart }.value,
            default = script.getArgDefault().trimSpace().apply { script = nextScriptPart }.value,
        )
    }

    private fun ScriptPart.getArgMode(): NextScript<Direction> {
        return when {
            restOfScript.startsWith("inout ", true) -> NextScript(Direction.INOUT, restOfScript.drop("inout ".length))
            restOfScript.startsWith("in ", true) -> NextScript(Direction.IN, restOfScript.drop("in ".length))
            restOfScript.startsWith("out ", true) -> NextScript(Direction.OUT, restOfScript.drop("out ".length))
            else -> NextScript(Direction.IN, restOfScript)
        }
    }

    private fun ScriptPart.getArgName(): NextScript<String> {
        try {
            return getNextScript { afterBeginBy(" ", "\n") }
        } catch (e: NameMalformed) {
            throw ArgNameMalformed(null, e)
        }
    }

    private fun ScriptPart.getArgType(): NextScript<ArgumentType> {
        val fullType = try {
            val endTextList = arrayOf(" default ", "=", ")")
            getNextScript { afterBeginBy(texts = endTextList) }
        } catch (e: ParseError) {
            throw ArgTypeMalformed(null, e)
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
            ArgumentType(
                name = name.value.trim(),
                precision = precision.value,
                scale = scale.value
            ), fullType.nextScriptPart.restOfScript
        )
    }

    private fun ScriptPart.getArgDefault(): NextScript<String?> {
        return if (this.isEmpty() || this.restOfScript == ")") {
            NextScript(null, "")
        } else {
            """^(\s*=\s*|\s+default\s+)(.+)\s*$"""
                .toRegex(IGNORE_CASE)
                .find(restOfScript)
                .let { it ?: throw ArgDefaultMalformed() }
                .let { it.groups[2]!!.value }
                .let { NextScript(it, "") }
        }
    }

    /**
     * TODO Finalize this
     */
    private fun ScriptPart.getReturns(): NextScript<Returns> {
        return NextScript(Returns.Void(), "")
    }

    class FunctionNotFound(cause: Throwable? = null): Resource.ParseException("Function not found in script", cause)
    class ArgumentNotFound(cause: Throwable? = null): Resource.ParseException("Argument not found in script", cause)
    class FunctionNameMalformed(message: String? = null, cause: Throwable? = null):
        Resource.ParseException(message ?: "Function name is malformed", cause)

    class ArgNameMalformed(message: String? = null, cause: Throwable? = null):
        Resource.ParseException(message ?: "Arg name is malformed", cause)

    class ArgTypeMalformed(message: String? = null, cause: Throwable? = null):
        Resource.ParseException(message ?: "Arg type is malformed", cause)

    class ArgDefaultMalformed(message: String? = null, cause: Throwable? = null):
        Resource.ParseException(message ?: "Arg default is malformed", cause)

    class NameMalformed(message: String? = null, cause: Throwable? = null):
        Resource.ParseException(message ?: "name is malformed", cause)

    class ParseError(message: String? = null, cause: Throwable? = null):
        Resource.ParseException(message ?: "Parsing fail", cause)

    fun getDefinition(): String = parameters
        .filter { it.direction == Direction.IN }
        .joinToString(", ") { it.type.toString() }
        .let { "$name ($it)" }

    fun getParametersIndexedByName(): Map<String, Parameter> = parameters
        .withIndex()
        .associate { (key, param) -> Pair(param.name ?: "${key + 1}", param) }

    operator fun get(name: String): Parameter? = parameters.firstOrNull { it.name == name }

    infix fun `has same definition`(other: Function): Boolean {
        return other.getDefinition() == this.getDefinition()
    }

    infix fun `is different from`(other: Function): Boolean {
        return other.script != this.script
    }

    sealed class Returns(
        val definition: String,
        val isSetOf: Boolean,
    ) {
        class Primitive(
            definition: String,
            isSetOf: Boolean,
        ): Returns(definition, isSetOf) {
            val name = definition
                .trim('"')
        }

        class PrimitiveList(
            definition: String,
            isSetOf: Boolean,
        ): Returns(definition, isSetOf) {
            val name = definition
                .drop(2)
                .trim('"')
        }

        class Table(
            definition: String,
            isSetOf: Boolean,
            val parameters: List<ParameterTable>,
        ): Returns(definition, isSetOf) {
            class ParameterTable(
                override val name: String,
                override val type: ArgumentType,
            ): ParameterSimpleI
        }

        class Any(
            isSetOf: Boolean,
        ): Returns("any", isSetOf)

        class Void: Returns("void", false)
    }

}
