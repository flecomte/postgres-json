package fr.postgresjson.definition

import java.util.Locale

class ArgumentType(
    val name: String,
    val precision: Int? = null,
    val scale: Int? = null,
    val isArray: Boolean = false,
) {
    override fun toString(): String {
        return if (precision == null && scale == null) {
            name
        } else if (scale == null) {
            """$name($precision)"""
        } else {
            """$name($precision, $scale)"""
        }
    }
}

interface ParameterSimpleI {
    val name: String?
    val type: ArgumentType
}

class Parameter(
    override val name: String?,
    override val type: ArgumentType,
    val direction: Direction = Direction.IN,
    val default: String? = null,
): ParameterSimpleI {
    constructor(name: String?, type: ArgumentType, direction: String = "IN", default: String? = null): this(
        name = name,
        type = type,
        direction = direction.let { Direction.valueOf(direction.uppercase(Locale.getDefault())) },
        default = default
    )

    enum class Direction { IN, OUT, INOUT }
}

interface ParametersInterface {
    val parameters: List<Parameter>
}
