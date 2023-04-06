package fr.postgresjson.connexion

import fr.postgresjson.utils.searchSqlFiles
import java.net.URI
import fr.postgresjson.definition.Query as QueryDefinition

/**
 * Convert [QueryDefinition], to runnable [Query]
 */
fun QueryDefinition.toRunnable(connection: Connection): Query = Query(name, script, connection)

/**
 * Convert Sequence of [QueryDefinition], to runnable Sequence of [Query]
 */
fun Sequence<QueryDefinition>.toRunnable(connection: Connection): Sequence<Query> = map { it.toRunnable(connection) }

/**
 * Convert Sequence of [Query], to [Map] of [Query] with name as key
 */
fun Sequence<Query>.toMutableMap(): MutableMap<String, Query> = map { it.name to it }.toMap().toMutableMap()

/**
 * Create a [Map] of [Query] from a [URI] pointing to the queries folder
 */
internal fun URI.toQuery(connection: Connection): MutableMap<String, Query> = searchSqlFiles()
    .filterIsInstance(QueryDefinition::class.java)
    .toRunnable(connection)
    .toMutableMap()
