package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.definition.Function
import fr.postgresjson.entity.EntityI

class Function(val definition: Function, override val connection: Connection): EmbedExecutable {
    override fun toString(): String {
        return definition.name
    }

    /* Select One */

    /**
     * Select One entity with list of parameters
     */
    override fun <R: EntityI<*>> select(
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? {
        val args = compileArgs(values)
        val sql = "SELECT * FROM ${definition.name} ($args)"

        return connection.select(sql, typeReference, values, block)
    }

    inline fun <reified R: EntityI<*>> selectOne(
        values: List<Any?> = emptyList(),
        noinline block: SelectOneCallback<R> = {}
    ): R? =
        select(object: TypeReference<R>() {}, values, block)

    /**
     * Select One entity with named parameters
     */
    override fun <R: EntityI<*>> select(
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? {
        val args = compileArgs(values)
        val sql = "SELECT * FROM ${definition.name} ($args)"

        return connection.select(sql, typeReference, values, block)
    }

    inline fun <reified R: EntityI<*>> selectOne(
        values: Map<String, Any?>,
        noinline block: SelectOneCallback<R> = {}
    ): R? =
        select(object: TypeReference<R>() {}, values, block)

    inline fun <reified R: EntityI<*>> selectOne(
        vararg values: Pair<String, Any?>,
        noinline block: SelectOneCallback<R> = {}
    ): R? =
        selectOne(values.toMap(), block)

    /* Select Multiples */

    /**
     * Select list of entities with list of parameters
     */
    override fun <R: EntityI<*>> select(
        typeReference: TypeReference<List<R>>,
        values: List<Any?>,
        block: (QueryResult, List<R>) -> Unit
    ): List<R> {
        val args = compileArgs(values)
        val sql = "SELECT * FROM ${definition.name} ($args)"

        return connection.select(sql, typeReference, values, block)
    }

    inline fun <reified R: EntityI<*>> select(
        values: List<Any?> = emptyList(),
        noinline block: SelectCallback<R> = {}
    ): List<R> =
        select(object: TypeReference<List<R>>() {}, values, block)

    /**
     * Select list of entities with named parameters
     */
    override fun <R: EntityI<*>> select(
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: (QueryResult, List<R>) -> Unit
    ): List<R> {
        val args = compileArgs(values)
        val sql = "SELECT * FROM ${definition.name} ($args)"

        return connection.select(sql, typeReference, values, block)
    }

    inline fun <reified R: EntityI<*>> select(
        values: Map<String, Any?>,
        noinline block: SelectCallback<R> = {}
    ): List<R> =
        select(object: TypeReference<List<R>>() {}, values, block)

    inline fun <reified R: EntityI<*>> select(
        vararg values: Pair<String, Any?>,
        noinline block: SelectCallback<R> = {}
    ): List<R> =
        select(values.toMap(), block)

    /* Select Paginated */

    /**
     * Select Multiple with pagination
     */
    override fun <R: EntityI<*>> select(
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

        val args = compileArgs(newValues)
        val sql = "SELECT * FROM ${definition.name} ($args)"

        return connection.select(sql, page, limit, typeReference, values, block)
    }

    inline fun <reified R: EntityI<*>> select(
        page: Int,
        limit: Int,
        values: Map<String, Any?> = emptyMap(),
        noinline block: SelectPaginatedCallback<R> = {}
    ): Paginated<R> =
        select(page, limit, object: TypeReference<List<R>>() {}, values, block)

    inline fun <reified R: EntityI<*>> select(
        page: Int,
        limit: Int,
        vararg values: Pair<String, Any?>,
        noinline block: SelectPaginatedCallback<R> = {}
    ): Paginated<R> =
        select(page, limit, object: TypeReference<List<R>>() {}, values.toMap(), block)

    /* Execute function without traitements */

    override fun exec(values: List<Any?>): QueryResult {
        val args = compileArgs(values)
        val sql = "SELECT * FROM ${definition.name} ($args)"

        return connection.exec(sql, values)
    }

    override fun exec(values: Map<String, Any?>): QueryResult {
        val args = compileArgs(values)
        val sql = "SELECT * FROM ${definition.name} ($args)"

        return connection.exec(sql, values)
    }

    private fun compileArgs(values: List<Any?>): String {
        val placeholders = values
            .filterIndexed { index, any ->
                definition.parameters[index].default === null || any !== null
            }
            .mapIndexed { index, any ->
                "?::" + definition.parameters[index].type
            }

        return placeholders.joinToString(separator = ", ")
    }

    private fun compileArgs(values: Map<String, Any?>): String {
        val parameters = definition.getParametersIndexedByName()
        val placeholders = values
            .filter { entry ->
                val parameter = parameters[entry.key] ?: error("Parameter ${entry.key} not exist")
                parameter.default === null || entry.value !== null
            }
            .map { entry ->
                val parameter = parameters[entry.key]!!
                """"${parameter.name}" := :${parameter.name}::${parameter.type}"""
            }

        return placeholders.joinToString(separator = ", ")
    }
}