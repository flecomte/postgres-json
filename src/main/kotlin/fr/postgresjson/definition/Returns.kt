package fr.postgresjson.definition

sealed class Returns(
    val definition: String,
    val isSetOf: Boolean,
) {
    class Primitive(
        definition: String,
        isSetOf: Boolean,
    ) : Returns(definition, isSetOf) {
        val name = definition
            .trim('"')
    }

    class PrimitiveList(
        definition: String,
        isSetOf: Boolean,
    ) : Returns(definition, isSetOf) {
        val name = definition
            .drop(2)
            .trim('"')
    }

    class Table(
        definition: String,
        isSetOf: Boolean,
        val parameters: List<ParameterTable>,
    ) : Returns(definition, isSetOf) {
        class ParameterTable(
            override val name: String,
            override val type: ParameterType,
        ) : ParameterSimpleI
    }

    class Any(
        isSetOf: Boolean,
    ) : Returns("any", isSetOf)

    class Unknown(
        definition: String,
        isSetOf: Boolean,
    ) : Returns(definition, isSetOf)

    class Void : Returns("void", false)
}
