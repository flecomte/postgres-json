package fr.postgresjson.definition

import java.util.Locale

class ParameterType(
    val name: String,
    val precision: Int? = null,
    val scale: Int? = null,
    val array: String? = null,
) {
    val isArray: Boolean
        get() = array != null

    override fun toString(): String {
        val type = if (precision == null && scale == null) {
            name
        } else if (scale == null) {
            """$name($precision)"""
        } else {
            """$name($precision, $scale)"""
        }

        return type+array
    }
}

interface ParameterSimpleI {
    val name: String?
    val type: ParameterType
}

class Parameter(
    override val name: String?,
    override val type: ParameterType,
    val direction: Direction = Direction.IN,
    val default: String? = null,
) : ParameterSimpleI {
    constructor(name: String?, type: ParameterType, direction: String = "IN", default: String? = null) : this(
        name = name,
        type = type,
        direction = direction.let { Direction.valueOf(direction.uppercase(Locale.getDefault())) },
        default = default
    )

    enum class Direction { IN, OUT, INOUT }

    override fun toString(): String {
        return buildString {
            append(direction.name.lowercase())
            if (name?.isNotBlank() == true) {
                append(" ")
                append(name)
            }
            append(" ")
            append(type.toString())
            if (default?.isNotBlank() == true) {
                append(" ")
                append(default)
            }
        }
    }
}

interface ParametersInterface {
    val parameters: List<Parameter>
}
