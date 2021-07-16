package fr.postgresjson.connexion

import fr.postgresjson.utils.searchSqlFiles
import java.net.URI
import fr.postgresjson.definition.Function as DefinitionFunction

fun DefinitionFunction.toRunnable(connection: Connection): Function = Function(this, connection)

fun Sequence<DefinitionFunction>.toRunnable(connection: Connection): Sequence<Function> = map { it.toRunnable(connection) }

fun Sequence<Function>.toMutableMap(): MutableMap<String, Function> = map { it.name to it }.toMap().toMutableMap()

internal fun URI.toFunction(connection: Connection): MutableMap<String, Function> = searchSqlFiles()
    .filterIsInstance(DefinitionFunction::class.java)
    .toRunnable(connection)
    .toMutableMap()
