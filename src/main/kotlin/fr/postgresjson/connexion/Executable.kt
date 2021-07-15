package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.entity.EntityI

interface Executable {

    /* Select One */

    /**
     * Update [EntityI] with one entity as argument
     */
    fun <R : EntityI> update(
        sql: String,
        typeReference: TypeReference<R>,
        value: R,
        block: SelectOneCallback<R> = {}
    ): R? =
        selectOne(sql, typeReference, listOf(value), block)

    /**
     * Select One [EntityI] with [List] of parameters
     */
    fun <R : EntityI> selectOne(
        sql: String,
        typeReference: TypeReference<R>,
        values: List<Any?> = emptyList(),
        block: SelectOneCallback<R> = {}
    ): R?

    /**
     * Select One [EntityI] with [Map] of parameters
     */
    fun <R : EntityI> selectOne(
        sql: String,
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: SelectOneCallback<R> = {}
    ): R?

    /**
     * Select One [EntityI] with multiple [Pair] of parameters
     */
    fun <R : EntityI> selectOne(
        sql: String,
        typeReference: TypeReference<R>,
        vararg values: Pair<String, Any?>,
        block: SelectOneCallback<R> = {}
    ): R? =
        selectOne(sql, typeReference, values.toMap(), block)

    /* Select Multiples */

    /**
     * Select Multiple [EntityI] with [List] of parameters
     */
    fun <R : EntityI> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: List<Any?> = emptyList(),
        block: SelectCallback<R> = {}
    ): List<R>

    /**
     * Select Multiple [EntityI] with [Map] of parameters
     */
    fun <R : EntityI> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: SelectCallback<R> = {}
    ): List<R>

    /**
     * Select Multiple [EntityI] with multiple [Pair] of parameters
     */
    fun <R : EntityI> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        vararg values: Pair<String, Any?>,
        block: SelectCallback<R> = {}
    ): List<R> =
        select(sql, typeReference, values.toMap(), block)

    /* Select Paginated */

    /**
     * Select Paginated [EntityI] with [Map] of parameters
     */
    fun <R : EntityI> select(
        sql: String,
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
        sql: String,
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        vararg values: Pair<String, Any?>,
        block: SelectPaginatedCallback<R> = {}
    ): Paginated<R> =
        select(sql, page, limit, typeReference, values.toMap(), block)

    fun <R : EntityI> exec(sql: String, value: R): QueryResult = exec(sql, listOf(value))
    fun exec(sql: String, values: List<Any?> = emptyList()): QueryResult
    fun exec(sql: String, values: Map<String, Any?>): QueryResult
    fun exec(sql: String, vararg values: Pair<String, Any?>): QueryResult = exec(sql, values.toMap())

    fun <R : EntityI> perform(sql: String, value: R) { perform(sql, listOf(value)) }
    fun perform(sql: String, values: List<Any?>) { exec(sql, values) }
    fun perform(sql: String, values: Map<String, Any?>) { exec(sql, values) }
    fun perform(sql: String, vararg values: Pair<String, Any?>) = perform(sql, values.toMap())

    fun <R : EntityI> sendQuery(sql: String, value: R): Int = sendQuery(sql, listOf(value))
    fun sendQuery(sql: String, values: List<Any?> = emptyList()): Int
    fun sendQuery(sql: String, values: Map<String, Any?>): Int
    fun sendQuery(sql: String, vararg values: Pair<String, Any?>): Int = sendQuery(sql, values.toMap())
}
