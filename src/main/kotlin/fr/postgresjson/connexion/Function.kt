package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.definition.Function

class Function(val definition: Function, override val connection: Connection) : EmbedExecutable {
    override fun toString(): String {
        return definition.name
    }

    override val name: String = definition.name

    /**
     * Select with [List] of parameters
     */
    override fun <R : Any> execute(
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: SelectCallback<R>
    ): R? =
        connection.execute(compileSql(values), typeReference, values, block)

    /**
     * Select with named parameters
     */
    override fun <R : Any> execute(
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: SelectCallback<R>
    ): R? =
        connection.execute(compileSql(values), typeReference, values, block)

    /**
     * Execute function without treatments
     */
    override fun exec(values: List<Any?>): QueryResult = connection.exec(compileSql(values), values)

    /**
     * Execute function without treatments
     */
    override fun exec(values: Map<String, Any?>): QueryResult = connection.exec(compileSql(values), values)

    private fun <A : Any?> compileParameters(value: A): String = compileParameters(listOf(value))

    /**
     * Add cast to all parameters
     */
    private fun compileParameters(values: List<Any?>): String {
        val placeholders = values
            .filterIndexed { index, value ->
                definition.parameters[index].default === null || value != null
            }
            .mapIndexed { index, _ ->
                "?::" + definition.parameters[index].type
            }

        return placeholders.joinToString(separator = ", ")
    }

    /**
     * Cast and add named parameters
     */
    private fun compileParameters(values: Map<String, Any?>): String {
        val parameters = definition.getParametersIndexedByName()
        val placeholders = values
            .filter { entry ->
                val parameter = parameters[entry.key] ?: parameters["_" + entry.key] ?: error("Parameter ${entry.key} of function ${definition.name} not exist")
                parameter.default === null || entry.value !== null
            }
            .map { entry ->
                val parameter = parameters[entry.key] ?: parameters["_" + entry.key] ?: error("Parameter ${entry.key} of function ${definition.name} not exist")
                """"${parameter.name}" := :${parameter.name}::${parameter.type}"""
            }

        return placeholders.joinToString(separator = ", ")
    }

    /**
     * Create SQL to call the function
     */
    private fun <A : Any?> compileSql(value: A): String = "SELECT * FROM ${definition.name} (${compileParameters(value)})"
    /**
     * Create SQL to call the function
     */
    private fun compileSql(values: List<Any?>): String = "SELECT * FROM ${definition.name} (${compileParameters(values)})"
    /**
     * Create SQL to call the function
     */
    private fun compileSql(values: Map<String, Any?>): String = "SELECT * FROM ${definition.name} (${compileParameters(values)})"
}
