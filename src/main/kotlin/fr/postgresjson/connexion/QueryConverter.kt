package fr.postgresjson.connexion

import fr.postgresjson.utils.searchSqlFiles
import java.net.URI
import fr.postgresjson.definition.Query as QueryDefinition

fun QueryDefinition.toConnection(connection: Connection): Query = Query(name, script, connection)

fun Sequence<QueryDefinition>.toConnection(connection: Connection): Sequence<Query> = map { it.toConnection(connection) }

fun Sequence<Query>.toMutableMap(): MutableMap<String, Query> = map { it.name to it }.toMap().toMutableMap()

internal fun URI.toQuery(connection: Connection): MutableMap<String, Query> = searchSqlFiles()
    .filterIsInstance(QueryDefinition::class.java)
    .toConnection(connection)
    .toMutableMap()
