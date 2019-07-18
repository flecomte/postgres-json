package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.entity.EntityI


class Query(private val sql: String, override val connection: Connection): EmbedExecutable {
    override fun toString(): String {
        return sql
    }

    override fun <R: EntityI<*>> select(typeReference: TypeReference<R>, values: List<Any?>, block: (QueryResult, R?) -> Unit): R? {
        return connection.select(this.toString(), typeReference, values, block)
    }

    inline fun <reified R: EntityI<*>> selectOne(values: List<Any?> = emptyList(), noinline block: SelectOneCallback<R> = {}): R? =
        select(object: TypeReference<R>() {}, values, block)

    override fun <R: EntityI<*>> select(typeReference: TypeReference<R>, values: Map<String, Any?>, block: (QueryResult, R?) -> Unit): R? {
        return connection.select(this.toString(), typeReference, values, block)
    }

    inline fun <reified R: EntityI<*>> selectOne(values: Map<String, Any?>, noinline block: SelectOneCallback<R> = {}): R? =
        select(object: TypeReference<R>() {}, values, block)

    override fun <R: EntityI<*>> select(typeReference: TypeReference<List<R>>, values: List<Any?>, block: (QueryResult, List<R>) -> Unit): List<R> {
        return connection.select(this.toString(), typeReference, values, block)
    }

    inline fun <reified R: EntityI<*>> select(values: List<Any?> = emptyList(), noinline block: SelectCallback<R> = {}): List<R> =
        select(object: TypeReference<List<R>>() {}, values, block)

    override fun <R: EntityI<*>> select(typeReference: TypeReference<List<R>>, values: Map<String, Any?>, block: (QueryResult, List<R>) -> Unit): List<R> {
        return connection.select(this.toString(), typeReference, values, block)
    }

    inline fun <reified R: EntityI<*>> select(values: Map<String, Any?>, noinline block: SelectCallback<R> = {}): List<R> =
        select(object: TypeReference<List<R>>() {}, values, block)

    override fun <R: EntityI<*>> select(page: Int, limit: Int, typeReference: TypeReference<List<R>>, values: Map<String, Any?>, block: (QueryResult, Paginated<R>) -> Unit): Paginated<R> {
        return connection.select(this.toString(), page, limit, typeReference, values, block)
    }
    inline fun <reified R: EntityI<*>> select(page: Int, limit: Int, values: Map<String, Any?> = emptyMap(), noinline block: SelectPaginatedCallback<R> = {}): Paginated<R> =
        select(page, limit, object: TypeReference<List<R>>() {}, values, block)

    override fun exec(values: List<Any?>): QueryResult {
        return connection.exec(sql, values)
    }

    override fun exec(values: Map<String, Any?>): QueryResult {
        return connection.exec(sql, values)
    }
}