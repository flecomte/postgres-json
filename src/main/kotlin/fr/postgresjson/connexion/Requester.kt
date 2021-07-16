package fr.postgresjson.connexion

import fr.postgresjson.utils.searchSqlFiles
import java.net.URI
import fr.postgresjson.definition.Function as DefinitionFunction
import fr.postgresjson.definition.Query as QueryDefinition

class Requester(
    private val connection: Connection,
    private val queries: MutableMap<String, Query> = mutableMapOf(),
    private val functions: MutableMap<String, Function> = mutableMapOf()
) {
    constructor(connection: Connection) : this(connection, mutableMapOf(), mutableMapOf())

    constructor(
        connection: Connection,
        queriesDirectory: URI? = null,
        functionsDirectory: URI? = null
    ) : this(
        connection = connection,
        queries = queriesDirectory?.toQuery(connection) ?: mutableMapOf(),
        functions = functionsDirectory?.toFunction(connection) ?: mutableMapOf(),
    )

    fun addQuery(query: Query) {
        queries[query.name] = query
    }

    fun addQuery(query: QueryDefinition) = addQuery(query.toRunnable(connection))

    fun addQuery(name: String, sql: String) {
        addQuery(Query(name, sql, connection))
    }

    fun addQuery(queriesDirectory: URI) {
        queriesDirectory
            .searchSqlFiles()
            .filterIsInstance(QueryDefinition::class.java)
            .forEach(this::addQuery)
    }

    fun getQueries(): List<Query> = queries.map { it.value }

    fun addFunction(definition: DefinitionFunction) {
        definition
            .run { toRunnable(connection) }
            .run { functions[name] = this }
    }

    fun addFunction(sql: String) {
        DefinitionFunction(sql)
            .run { toRunnable(connection) }
            .run { functions[name] = this }
    }

    fun addFunctions(functionsDirectory: URI) {
        functionsDirectory.searchSqlFiles()
            .filterIsInstance(DefinitionFunction::class.java)
            .forEach(this::addFunction)
    }

    fun getFunction(name: String): Function = functions[name] ?: throw NoFunctionDefined(name)

    fun getQuery(path: String): Query = queries[path] ?: throw NoQueryDefined(path)

    class NoFunctionDefined(name: String) : Exception("No function defined for $name")
    class NoQueryDefined(path: String) : Exception("No query defined in $path")
}
