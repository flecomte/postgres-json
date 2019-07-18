package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.entity.EntityI

interface EmbedExecutable {
    val connection: Connection
    override fun toString(): String

    /* Select One */
    /**
     * Select One entity with list of parameters
     */
    fun <R: EntityI<*>> select(
        typeReference: TypeReference<R>,
        values: List<Any?> = emptyList(),
        block: SelectOneCallback<R> = {}
    ): R?

    fun <R: EntityI<*>> select(
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: SelectOneCallback<R> = {}
    ): R?

    /* Select Miltiples */
    fun <R: EntityI<*>> select(
        typeReference: TypeReference<List<R>>,
        values: List<Any?> = emptyList(),
        block: SelectCallback<R> = {}
    ): List<R>

    fun <R: EntityI<*>> select(
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: SelectCallback<R> = {}
    ): List<R>

    /* Select Paginated */
    fun <R: EntityI<*>> select(
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: SelectPaginatedCallback<R> = {}
    ): Paginated<R>

    fun exec(values: List<Any?> = emptyList()): QueryResult
    fun exec(values: Map<String, Any?>): QueryResult
}