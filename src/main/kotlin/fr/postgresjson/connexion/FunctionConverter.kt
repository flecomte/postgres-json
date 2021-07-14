package fr.postgresjson.connexion

import fr.postgresjson.utils.searchSqlFiles
import java.net.URI
import fr.postgresjson.definition.Function as DefinitionFunction

fun DefinitionFunction.toConnection(connection: Connection): Function = Function(this, connection)

fun Sequence<DefinitionFunction>.toConnection(connection: Connection): Sequence<Function> = map { it.toConnection(connection) }

fun Sequence<Function>.toMutableMap(): MutableMap<String, Function> = map { it.name to it }.toMap().toMutableMap()

internal fun URI.toFunction(connection: Connection): MutableMap<String, Function> = searchSqlFiles()
    .filterIsInstance(DefinitionFunction::class.java)
    .toConnection(connection)
    .toMutableMap()
