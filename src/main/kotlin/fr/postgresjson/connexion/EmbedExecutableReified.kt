package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.entity.EntityI

/* Select One */

inline fun <reified R : EntityI> EmbedExecutable.update(
    value: R,
    noinline block: SelectOneCallback<R> = {}
): R? =
    update(object : TypeReference<R>() {}, value, block)

inline fun <reified R : EntityI> EmbedExecutable.selectOne(
    values: List<Any?> = emptyList(),
    noinline block: SelectOneCallback<R> = {}
): R? =
    selectOne(object : TypeReference<R>() {}, values, block)

inline fun <reified R : EntityI> EmbedExecutable.selectOne(
    values: Map<String, Any?>,
    noinline block: SelectOneCallback<R> = {}
): R? =
    selectOne(object : TypeReference<R>() {}, values, block)

inline fun <reified R : EntityI> EmbedExecutable.selectOne(
    vararg values: Pair<String, Any?>,
    noinline block: SelectOneCallback<R> = {}
): R? =
    selectOne(object : TypeReference<R>() {}, values = values, block)

/* Select Multiples */

inline fun <reified R : EntityI> EmbedExecutable.select(
    values: List<Any?> = emptyList(),
    noinline block: SelectCallback<R> = {}
): List<R> =
    select(object : TypeReference<List<R>>() {}, values, block)

inline fun <reified R : EntityI> EmbedExecutable.select(
    values: Map<String, Any?>,
    noinline block: SelectCallback<R> = {}
): List<R> =
    select(object : TypeReference<List<R>>() {}, values, block)

inline fun <reified R : EntityI> EmbedExecutable.select(
    vararg values: Pair<String, Any?>,
    noinline block: SelectCallback<R> = {}
): List<R> =
    select(object : TypeReference<List<R>>() {}, values = values, block)

/* Select Paginated */

inline fun <reified R : EntityI> EmbedExecutable.select(
    page: Int,
    limit: Int,
    values: Map<String, Any?> = emptyMap(),
    noinline block: SelectPaginatedCallback<R> = {}
): Paginated<R> =
    select(page, limit, object : TypeReference<List<R>>() {}, values, block)

inline fun <reified R : EntityI> EmbedExecutable.select(
    page: Int,
    limit: Int,
    vararg values: Pair<String, Any?>,
    noinline block: SelectPaginatedCallback<R> = {}
): Paginated<R> =
    select(page, limit, object : TypeReference<List<R>>() {}, values = values, block)
