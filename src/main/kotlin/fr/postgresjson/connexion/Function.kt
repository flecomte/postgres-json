package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.definition.Function
import fr.postgresjson.entity.EntityI

class Function(val definition: Function, override val connection: Connection) : EmbedExecutable {
    override fun toString(): String {
        return definition.name
    }

    override val name: String = definition.name

    /* Select One */

    /**
     * Select One [EntityI] with [List] of parameters
     */
    override fun <R : EntityI> selectOne(
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? =
        connection.selectOne(compileSql(values), typeReference, values, block)

    /**
     * Select One [EntityI] with named parameters
     */
    override fun <R : EntityI> selectOne(
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? =
        connection.selectOne(compileSql(values), typeReference, values, block)

    /* Select Multiples */

    /**
     * Select multiple [EntityI] with [List] of parameters
     */
    override fun <R : EntityI> select(
        typeReference: TypeReference<List<R>>,
        values: List<Any?>,
        block: (QueryResult, List<R>) -> Unit
    ): List<R> =
        connection.select(compileSql(values), typeReference, values, block)

    /**
     * Select multiple [EntityI] with named parameters
     */
    override fun <R : EntityI> select(
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: (QueryResult, List<R>) -> Unit
    ): List<R> =
        connection.select(compileSql(values), typeReference, values, block)

    /* Select Paginated */

    /**
     * Select Multiple [EntityI] with pagination
     */
    override fun <R : EntityI> select(
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: (QueryResult, Paginated<R>) -> Unit
    ): Paginated<R> {
        val offset = (page - 1) * limit
        val newValues = values
            .plus("offset" to offset)
            .plus("limit" to limit)

        return connection.select(compileSql(newValues), page, limit, typeReference, values, block)
    }

    /* Execute function without treatments */

    override fun exec(values: List<Any?>): QueryResult = connection.exec(compileSql(values), values)

    override fun exec(values: Map<String, Any?>): QueryResult = connection.exec(compileSql(values), values)

    /**
     * Warning: this method not use prepared statement
     */
    override fun sendQuery(values: List<Any?>): Int {
        exec(values)
        return 0
    }

    /**
     * Warning: this method not use prepared statement
     */
    override fun sendQuery(values: Map<String, Any?>): Int {
        exec(values)
        return 0
    }

    private fun <R : EntityI> compileArgs(value: R): String = compileArgs(listOf(value))

    private fun compileArgs(values: List<Any?>): String {
        val placeholders = values
            .filterIndexed { index, value ->
                definition.parameters[index].default === null || value != null
            }
            .mapIndexed { index, _ ->
                "?::" + definition.parameters[index].type
            }

        return placeholders.joinToString(separator = ", ")
    }

    private fun compileArgs(values: Map<String, Any?>): String {
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

    private fun <R : EntityI> compileSql(value: R): String = "SELECT * FROM ${definition.name} (${compileArgs(value)})"
    private fun compileSql(values: List<Any?>): String = "SELECT * FROM ${definition.name} (${compileArgs(values)})"
    private fun compileSql(values: Map<String, Any?>): String = "SELECT * FROM ${definition.name} (${compileArgs(values)})"
}
