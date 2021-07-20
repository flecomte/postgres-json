package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.entity.EntityI

sealed interface EmbedExecutable {
    val connection: Connection
    override fun toString(): String
    val name: String

    /* Select One */

    /**
     * Update [EntityI] with one entity as argument
     */
    fun <R : EntityI> update(
        typeReference: TypeReference<R>,
        value: R,
        block: SelectOneCallback<R> = {}
    ): R? =
        selectOne(typeReference, listOf(value), block)

    /**
     * Select One [EntityI] with [List] of parameters
     */
    fun <R : EntityI> selectOne(
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: SelectOneCallback<R> = {}
    ): R?

    /**
     * Select One [EntityI] with [Map] of parameters
     */
    fun <R : EntityI> selectOne(
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: SelectOneCallback<R> = {}
    ): R?

    /**
     * Select One [EntityI] with multiple [Pair] of parameters
     */
    fun <R : EntityI> selectOne(
        typeReference: TypeReference<R>,
        vararg values: Pair<String, Any?>,
        block: SelectOneCallback<R> = {}
    ): R? =
        selectOne(typeReference, values.toMap(), block)

    /* Select Multiples */

    /**
     * Select Multiple [EntityI] with [List] of parameters
     */
    fun <R : EntityI> select(
        typeReference: TypeReference<List<R>>,
        values: List<Any?>,
        block: SelectCallback<R> = {}
    ): List<R>

    /**
     * Select Multiple [EntityI] with [Map] of parameters
     */
    fun <R : EntityI> select(
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: SelectCallback<R> = {}
    ): List<R>

    /**
     * Select Multiple [EntityI] with multiple [Pair] of parameters
     */
    fun <R : EntityI> select(
        typeReference: TypeReference<List<R>>,
        vararg values: Pair<String, Any?>,
        block: SelectCallback<R> = {}
    ): List<R> =
        select(typeReference, values.toMap(), block)

    /* Select Paginated */

    /**
     * Select Paginated [EntityI] with [Map] of parameters
     */
    fun <R : EntityI> select(
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: SelectPaginatedCallback<R> = {}
    ): Paginated<R>

    /**
     * Select Paginated [EntityI] with multiple [Pair] of parameters
     */
    fun <R : EntityI> select(
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        vararg values: Pair<String, Any?>,
        block: SelectPaginatedCallback<R> = {}
    ): Paginated<R> =
        select(page, limit, typeReference, values.toMap(), block)

    fun exec(values: List<Any?>): QueryResult
    fun exec(values: Map<String, Any?>): QueryResult
    fun exec(vararg values: Pair<String, Any?>): QueryResult = exec(values.toMap())
}
