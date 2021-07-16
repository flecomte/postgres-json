package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.entity.EntityI

/* Update */

/**
 * Update [EntityI] with one entity as argument
 */
inline fun <reified R : EntityI> Executable.update(
    sql: String,
    value: R,
    noinline block: SelectOneCallback<R> = {}
): R? =
    update(sql, object : TypeReference<R>() {}, value, block)

/* Select One */

/**
 * Select One [EntityI] with [List] of parameters
 */
inline fun <reified R : EntityI> Executable.selectOne(
    sql: String,
    values: List<Any?> = emptyList(),
    noinline block: SelectOneCallback<R> = {}
): R? =
    selectOne(sql, object : TypeReference<R>() {}, values, block)

/**
 * Select One [EntityI] with [Map] of parameters
 */
inline fun <reified R : EntityI> Executable.selectOne(
    sql: String,
    values: Map<String, Any?>,
    noinline block: SelectOneCallback<R> = {}
): R? =
    selectOne(sql, object : TypeReference<R>() {}, values, block)

/**
 * Select One [EntityI] with multiple [Pair] of parameters
 */
inline fun <reified R : EntityI> Executable.selectOne(
    sql: String,
    vararg values: Pair<String, Any?>,
    noinline block: SelectOneCallback<R> = {}
): R? =
    selectOne(sql, object : TypeReference<R>() {}, values = values, block)

/* Select Multiples */

/**
 * Select Multiple [EntityI] with [List] of parameters
 */
inline fun <reified R : EntityI> Executable.select(
    sql: String,
    values: List<Any?> = emptyList(),
    noinline block: SelectCallback<R> = {}
): List<R> =
    select(sql, object : TypeReference<List<R>>() {}, values, block)

/**
 * Select Multiple [EntityI] with [Map] of parameters
 */
inline fun <reified R : EntityI> Executable.select(
    sql: String,
    values: Map<String, Any?>,
    noinline block: SelectCallback<R> = {}
): List<R> =
    select(sql, object : TypeReference<List<R>>() {}, values, block)

/**
 * Select Multiple [EntityI] with multiple [Pair] of parameters
 */
inline fun <reified R : EntityI> Executable.select(
    sql: String,
    vararg values: Pair<String, Any?>,
    noinline block: SelectCallback<R> = {}
): List<R> =
    select(sql, object : TypeReference<List<R>>() {}, values = values, block)

/* Select Paginated */

/**
 * Select Paginated [EntityI] with [Map] of parameters
 */
inline fun <reified R : EntityI> Executable.select(
    sql: String,
    page: Int,
    limit: Int,
    values: Map<String, Any?> = emptyMap(),
    noinline block: SelectPaginatedCallback<R> = {}
): Paginated<R> =
    select(sql, page, limit, object : TypeReference<List<R>>() {}, values, block)

/**
 * Select Paginated [EntityI] with multiple [Pair] of parameters
 */
inline fun <reified R : EntityI> Executable.select(
    sql: String,
    page: Int,
    limit: Int,
    vararg values: Pair<String, Any?>,
    noinline block: SelectPaginatedCallback<R> = {}
): Paginated<R> =
    select(sql, page, limit, object : TypeReference<List<R>>() {}, values = values, block)
