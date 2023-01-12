package fr.postgresjson.definition

import java.util.Locale

interface ParameterI {
    val name: String
    val type: String
    val direction: Parameter.Direction
    val default: String
}

class Parameter(val name: String, val type: String, direction: Direction? = Direction.IN, val default: String? = null, val precision: Int? = null, val scale: Int? = null) {
    val direction: Direction

    init {
        if (direction === null) {
            this.direction = Direction.IN
        } else {
            this.direction = direction
        }
    }

    constructor(name: String, type: String, direction: String? = "IN", default: String? = null, precision: Int? = null, scale: Int? = null) : this(
        name = name,
        type = type,
        direction = direction?.let { Direction.valueOf(direction.uppercase(Locale.getDefault())) },
        default = default,
        precision = precision,
        scale = scale
    )

    enum class Direction { IN, OUT, INOUT }
}

interface ParametersInterface {
    val parameters: List<Parameter>
}
