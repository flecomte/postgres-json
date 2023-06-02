package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult

class Query(override val name: String, private val sql: String, override val connection: Connection) : EmbedExecutable {
    override fun toString(): String {
        return sql
    }

    /**
     * Select with unnamed of parameters
     */
    override fun <R : Any> execute(
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: SelectCallback<R>
    ): R? =
        connection.execute(sql, typeReference, values, block)

    /**
     * Select with named parameters
     */
    override fun <R : Any> execute(
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: SelectCallback<R>
    ): R? =
        connection.execute(sql, typeReference, values, block)

    /**
     * Execute function without treatments
     */
    override fun exec(values: List<Any?>): QueryResult = connection.exec(sql, values)

    /**
     * Execute function without treatments
     */
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
