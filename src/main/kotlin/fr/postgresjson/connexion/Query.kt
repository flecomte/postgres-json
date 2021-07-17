package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.entity.EntityI

class Query(override val name: String, private val sql: String, override val connection: Connection) : EmbedExecutable {
    override fun toString(): String {
        return sql
    }

    /* Select One */

    /**
     * Select One [EntityI] with [List] of parameters
     */
    override fun <R : EntityI> selectOne(
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? =
        connection.selectOne(sql, typeReference, values, block)

    /**
     * Select One [EntityI] with named parameters
     */
    override fun <R : EntityI> selectOne(
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: (QueryResult, R?) -> Unit
    ): R? =
        connection.selectOne(sql, typeReference, values, block)

    /* Select Multiples */

    /**
     * Select multiple [EntityI] with [List] of parameters
     */
    override fun <R : EntityI> select(
        typeReference: TypeReference<List<R>>,
        values: List<Any?>,
        block: (QueryResult, List<R>) -> Unit
    ): List<R> =
        connection.select(sql, typeReference, values, block)

    /**
     * Select multiple [EntityI] with [Map] of parameters
     */
    override fun <R : EntityI> select(
        typeReference: TypeReference<List<R>>,
        values: Map<String, Any?>,
        block: (QueryResult, List<R>) -> Unit
    ): List<R> =
        connection.select(sql, typeReference, values, block)

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
    ): Paginated<R> =
        connection.select(sql, page, limit, typeReference, values, block)

    /* Execute function without treatments */

    override fun exec(values: List<Any?>): QueryResult = connection.exec(sql, values)

    override fun exec(values: Map<String, Any?>): QueryResult = connection.exec(sql, values)

    /**
     * Warning: this method not use prepared statement
     */
    fun sendQuery(values: List<Any?>): QueryResult = connection.sendQuery(sql, values)

    /**
     * Warning: this method not use prepared statement
     */
    fun sendQuery(values: Map<String, Any?>): QueryResult = connection.sendQuery(sql, values)
    /**
     * Warning: this method not use prepared statement
     */
    fun sendQuery(vararg values: Pair<String, Any?>): QueryResult = sendQuery(values.toMap())
}
