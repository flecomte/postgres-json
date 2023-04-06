package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import kotlin.jvm.Throws

sealed interface EmbedExecutable : Executable {
    val connection: Connection
    override fun toString(): String
    val name: String

    /**
     * Select with unnamed parameters
     */
    @Throws(DataNotFoundException::class)
    fun <R : Any> execute(
        typeReference: TypeReference<R>,
        values: List<Any?>,
        block: SelectCallback<R> = {}
    ): R?

    /**
     * Select with named parameters
     */
    @Throws(DataNotFoundException::class)
    fun <R : Any> execute(
        typeReference: TypeReference<R>,
        values: Map<String, Any?>,
        block: SelectCallback<R> = {}
    ): R?

    /**
     * Select with named parameters
     */
    @Throws(DataNotFoundException::class)
    fun <R : Any> execute(
        typeReference: TypeReference<R>,
        vararg values: Pair<String, Any?>,
        block: SelectCallback<R> = {}
    ): R? =
        execute(typeReference, values.toMap(), block)

    fun exec(values: List<Any?>): QueryResult
    fun exec(values: Map<String, Any?>): QueryResult
    fun exec(vararg values: Pair<String, Any?>): QueryResult = exec(values.toMap())
}
