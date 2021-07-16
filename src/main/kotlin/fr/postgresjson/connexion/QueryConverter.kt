package fr.postgresjson.connexion

import fr.postgresjson.utils.searchSqlFiles
import java.net.URI
import fr.postgresjson.definition.Query as QueryDefinition

fun QueryDefinition.toRunnable(connection: Connection): Query = Query(name, script, connection)

fun Sequence<QueryDefinition>.toRunnable(connection: Connection): Sequence<Query> = map { it.toRunnable(connection) }

fun Sequence<Query>.toMutableMap(): MutableMap<String, Query> = map { it.name to it }.toMap().toMutableMap()

internal fun URI.toQuery(connection: Connection): MutableMap<String, Query> = searchSqlFiles()
    .filterIsInstance(QueryDefinition::class.java)
    .toRunnable(connection)
    .toMutableMap()
