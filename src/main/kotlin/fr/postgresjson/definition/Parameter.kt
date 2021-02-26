package fr.postgresjson.definition

interface ParameterI {
    val name: String
    val type: String
    val direction: Parameter.Direction
    val default: String
}

class Parameter(val name: String, val type: String, direction: Direction? = Direction.IN, val default: Any? = null) {
    val direction: Direction

    init {
        if (direction === null) {
            this.direction = Direction.IN
        } else {
            this.direction = direction
        }
    }

    constructor(name: String, type: String, direction: String? = "IN", default: Any? = null) : this(
        name = name,
        type = type,
        direction = direction?.let { Direction.valueOf(direction.toUpperCase()) },
        default = default
    )

    enum class Direction { IN, OUT, INOUT }
}

interface ParametersInterface {
    val parameters: List<Parameter>
}
