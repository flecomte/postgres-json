package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import kotlin.jvm.Throws

@Throws(DataNotFoundException::class)
inline fun <reified R : Any> EmbedExecutable.execute(
    values: List<Any?>,
    noinline block: SelectCallback<R> = {}
): R? =
    execute(object : TypeReference<R>() {}, values, block)

@Throws(DataNotFoundException::class)
inline fun <reified R : Any> EmbedExecutable.execute(
    values: Map<String, Any?>,
    noinline block: SelectCallback<R> = {}
): R? =
    execute(object : TypeReference<R>() {}, values, block)

@Throws(DataNotFoundException::class)
inline fun <reified R : Any> EmbedExecutable.execute(
    vararg values: Pair<String, Any?>,
    noinline block: SelectCallback<R> = {}
): R? =
    execute(object : TypeReference<R>() {}, values = values, block)
