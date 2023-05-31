package fr.postgresjson.definition

import fr.postgresjson.definition.Parameter.Direction.IN
import fr.postgresjson.definition.parse.ScriptPart
import fr.postgresjson.definition.parse.trimSpace
import java.nio.file.Path

class Function(
    override val name: String,
    override val parameters: List<Parameter>,
    val returns: Returns,
    override val script: String,
    override val source: Path? = null,
): Resource, ParametersInterface {

//    private fun <T> NextScript<T>.changeValue(block: (T) -> T): NextScript<T> {
//        return NextScript(block(value), restOfScript)
//    }
//
//    private fun <T> NextScript<T>.changeScript(block: (String) -> String): NextScript<T> {
//        return NextScript(value, block(restOfScript))
//    }
//
//    private fun <T> NextScript<T>.dropOneOf(vararg endTextList: String): NextScript<T> {
//        return changeScript { script ->
//            endTextList
//                .filter { script.startsWith(it) }
//                .let { script.drop(it.size) }
//        }
//    }

    fun getDefinition(): String = parameters
        .filter { it.direction == IN }
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
}
