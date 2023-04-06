package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import kotlin.jvm.Throws

typealias SelectCallback<R> = QueryResult.(R?) -> Unit

sealed interface ExecutableRaw : Executable {
    /**
     * Select with one entity as argument
     */
    @Throws(DataNotFoundException::class)
    fun <R : Any> execute(
        sql: String,
        typeReference: TypeReference<R>,
        value: R,
        block: SelectCallback<R> = {}
    ): R? =
        execute(sql, typeReference, listOf(value), block)

    /**
     * Select with [List] of parameters
     */
    @Throws(DataNotFoundException::class)
    fun <R : Any> execute(
        sql: String,
        typeReference: TypeReference<R>,
        values: List<Any?> = emptyList(),
        block: SelectCallback<R> = {}
    ): R?

    /**
     * Select with [Map] of parameters
     */
    @Throws(DataNotFoundException::class)
    fun <R : Any> execute(
        sql: String,
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: SelectCallback<R> = {}
    ): R?

    /**
     * Select with multiple [Pair] of parameters
     */
    @Throws(DataNotFoundException::class)
    fun <R : Any> execute(
        sql: String,
        typeReference: TypeReference<R>,
        vararg values: Pair<String, Any?>,
        block: SelectCallback<R> = {}
    ): R? = execute(sql, typeReference, values.toMap(), block)

    fun <R : Any?> exec(sql: String, value: R): QueryResult = exec(sql, listOf(value))
    fun exec(sql: String, values: List<Any?>): QueryResult
    fun exec(sql: String, values: Map<String, Any?>): QueryResult
    fun exec(sql: String, vararg values: Pair<String, Any?>): QueryResult = exec(sql, values.toMap())

    /**
     * Warning: this method not use prepared statement
     */
    fun <R : Any?> sendQuery(sql: String, value: R): QueryResult = sendQuery(sql, listOf(value))
    /**
     * Warning: this method not use prepared statement
     */
    fun sendQuery(sql: String, values: List<Any?>): QueryResult
    /**
     * Warning: this method not use prepared statement
     */
    fun sendQuery(sql: String, values: Map<String, Any?>): QueryResult
    /**
     * Warning: this method not use prepared statement
     */
    fun sendQuery(sql: String, vararg values: Pair<String, Any?>): QueryResult = sendQuery(sql, values.toMap())
}
