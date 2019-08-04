package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.entity.EntityI
import java.sql.ResultSet

interface Executable {
    /* Select One */

    fun <R: EntityI<*>> select(
        sql: String,
        typeReference: TypeReference<R>,
        values: List<Any?> = emptyList(),
        block: SelectOneCallback<R> = {}
    ): R?

    fun <R: EntityI<*>> select(
        sql: String,
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: SelectOneCallback<R> = {}
    ): R?

    /* Select Miltiples */

    fun <R: EntityI<*>> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: List<Any?> = emptyList(),
        block: SelectCallback<R> = {}
    ): List<R>

    fun <R: EntityI<*>> select(
        sql: String,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: SelectCallback<R> = {}
    ): List<R>

    /* Select Paginated */

    fun <R: EntityI<*>> select(
        sql: String,
        page: Int,
        limit: Int,
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: SelectPaginatedCallback<R> = {}
    ): Paginated<R>

    fun exec(sql: String, values: List<Any?> = emptyList()): ResultSet
    fun exec(sql: String, values: Map<String, Any?>): ResultSet
    fun sendQuery(sql: String, values: List<Any?> = emptyList()): Int
}