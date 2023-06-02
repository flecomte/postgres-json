package fr.postgresjson.definition.parse

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

@JvmInline
internal value class ScriptPart(val restOfScript: String) {
    fun copy(block: (String) -> String): ScriptPart {
        return ScriptPart(block(restOfScript))
    }

    fun isEmpty() = restOfScript.isEmpty()
}

internal class NextScript<T>(val value: T, val restOfScript: String) {
    val nextScriptPart: ScriptPart = ScriptPart(restOfScript)
    fun isLast() = restOfScript == ""
    fun isEmptyValue() = value == "" || value == null
}

internal fun ScriptPart.removeParentheses(): ScriptPart {
    return if (restOfScript.take(1) == "(" && restOfScript.takeLast(1) == ")") {
        this.copy {
            it.drop(1).dropLast(1)
        }
    } else {
        this
    }
}

/**
 * Get next part of script.
 * You can define a list of characters that end the part of script. Like `(` or space.
 */
@Throws(ParseError::class)
internal fun ScriptPart.getNextScript(isEnd: Context.() -> Boolean = { false }): NextScript<String> {
    val status = Status()

    for ((index, c) in restOfScript.withIndex()) {
        val prevChar = restOfScript.getOrNull(index - 1)
        val nextChar = restOfScript.getOrNull(index + 1)
        val nestedChars = listOf(prevChar, nextChar)

        if (c == '"' && nestedChars.none { c == it }) {
            status.doubleQuoted = !status.doubleQuoted
        } else if (c == '\'' && nestedChars.none { c == it }) {
            status.simpleQuoted = !status.simpleQuoted
        }

        if (status.isNotQuoted()) {
            when (c) {
                '(' -> status.parentheses++
                ')' -> status.parentheses--
                '[' -> status.brackets++
                ']' -> status.brackets--
                '{' -> status.braces++
                '}' -> status.braces--
            }
        }

        if (isEnd(Context(index, c, status.copy(), restOfScript))) {
            return NextScript(
                restOfScript.take(index + 1),
                restOfScript.drop(index + 1),
            )
        }
    }

    if (status.isNotEscaped()) {
        return NextScript(
            restOfScript.trim(),
            "",
        )
    }

    throw ParseError()
}

internal fun ScriptPart.unescapeOrLowercase(): ScriptPart = restOfScript
    .run(String::unescapeOrLowercase)
    .let(::ScriptPart)

internal fun String.unescapeOrLowercase(): String {
    val first = take(1)
    val last = takeLast(1)
    return if (first == last && first == "'") {
        drop(1).dropLast(1).replace("$first$first", first).lowercase()
    } else if (first == last && first == "\"") {
        drop(1).dropLast(1).replace("$first$first", first)
    } else {
        this.lowercase()
    }
}

internal fun <T> NextScript<T>.trimSpace(): NextScript<T> {
    val spaces = charArrayOf(' ', '\n', '\t')
    return trim(chars = spaces)
}

internal fun ScriptPart.trimSpace(): ScriptPart {
    for ((n, char) in restOfScript.withIndex()) {
        if (char !in listOf(' ', '\n', '\t')) {
            return ScriptPart(
                restOfScript.drop(n)
            )
        }
    }
    return ScriptPart(restOfScript)
}

internal fun <T> NextScript<T>.trim(vararg chars: Char): NextScript<T> {
    return NextScript(value, restOfScript.apply { dropWhile { it in chars } })
}

internal fun ScriptPart.trimStart(vararg chars: Char): ScriptPart {
    return this.change { dropWhile { it in chars } }
}

internal fun ScriptPart.trimEnd(vararg chars: Char): ScriptPart {
    return this.change { dropLastWhile { it in chars } }
}

internal fun ScriptPart.split(delimiter: String): List<ScriptPart> {
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

/**
 * Return the value as ScriptPart
 */
internal fun NextScript<String>.valueAsScriptPart(): ScriptPart = ScriptPart(value)

@OptIn(ExperimentalContracts::class)
internal inline fun ScriptPart.change(block: String.() -> String): ScriptPart {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }
    return ScriptPart(restOfScript.run(block))
}

@OptIn(ExperimentalContracts::class)
internal inline fun <T> NextScript<T>.changeValue(block: T.() -> T): NextScript<T> {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }
    return NextScript(value.run(block), nextScriptPart.restOfScript)
}

internal fun ScriptPart.getNextInteger(): NextScript<Int?> {
    val digits = restOfScript.takeWhile { it.isDigit() }
    val restOfScript = restOfScript.trimStart { it.isDigit() }
    return NextScript(digits.toIntOrNull(), restOfScript).trimSpace()
}

internal data class Status(
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

internal data class Context(
    val index: Int,
    val currentChar: Char,
    val status: Status,
    val script: String,
) {
    fun afterBeginBy(vararg texts: String): Boolean = texts.any {
        script.drop(index + 1).take(it.length).lowercase() == it.lowercase()
    }
    fun afterBeginBy(vararg texts: Regex): Boolean = texts.any {
        it.matchAt(script, index + 1) != null
    }

    val nextChar: Char? get() = script.substring(index + 1).getOrNull(0)
}
