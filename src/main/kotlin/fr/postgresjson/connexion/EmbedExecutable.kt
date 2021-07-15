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
     * Select One entity with list of parameters
     */
    fun <R : EntityI> selectOne(
        typeReference: TypeReference<R>,
        values: List<Any?> = emptyList(),
        block: SelectOneCallback<R> = {}
    ): R?

    fun <R : EntityI> selectOne(
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: SelectOneCallback<R> = {}
    ): R?

    /* Select Multiples */
    fun <R : EntityI> select(
        typeReference: TypeReference<List<R>>,
        values: List<Any?> = emptyList(),
        block: SelectCallback<R> = {}
    ): List<R>

    fun <R : EntityI> select(
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: SelectCallback<R> = {}
    ): List<R>

    /* Select Paginated */
    fun <R : EntityI> select(
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: SelectPaginatedCallback<R> = {}
    ): Paginated<R>

    fun exec(values: List<Any?> = emptyList()): QueryResult
    fun exec(values: Map<String, Any?>): QueryResult
    fun exec(vararg values: Pair<String, Any?>): QueryResult = exec(values.toMap())

    fun perform(values: List<Any?>) { exec(values) }
    fun perform(values: Map<String, Any?>) { exec(values) }
    fun perform(vararg values: Pair<String, Any?>) = perform(values.toMap())

    fun sendQuery(values: List<Any?> = emptyList()): Int
    fun sendQuery(values: Map<String, Any?>): Int
    fun sendQuery(vararg values: Pair<String, Any?>): Int =
        sendQuery(values.toMap())
}
