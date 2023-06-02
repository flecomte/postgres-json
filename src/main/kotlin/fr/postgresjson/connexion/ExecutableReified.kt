package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import kotlin.jvm.Throws

/**
 * Select with unnamed parameters
 */
@Throws(DataNotFoundException::class)
inline fun <reified R : Any> ExecutableRaw.execute(
    sql: String,
    values: List<Any?> = emptyList(),
    noinline block: SelectCallback<R> = {}
): R? =
    execute(sql, object : TypeReference<R>() {}, values, block)

/**
 * Select with named parameters
 */
@Throws(DataNotFoundException::class)
inline fun <reified R : Any> ExecutableRaw.execute(
    sql: String,
    values: Map<String, Any?>,
    noinline block: SelectCallback<R> = {}
): R? =
    execute(sql, object : TypeReference<R>() {}, values, block)

/**
 * Select with named parameters
 */
@Throws(DataNotFoundException::class)
inline fun <reified R : Any> ExecutableRaw.execute(
    sql: String,
    vararg values: Pair<String, Any?>,
    noinline block: SelectCallback<R> = {}
): R? =
    execute(sql, object : TypeReference<R>() {}, values = values, block)
