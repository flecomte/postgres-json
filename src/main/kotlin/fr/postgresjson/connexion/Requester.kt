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

    fun addQuery(query: Query): Requester {
        queries[query.name] = query
        return this
    }

    fun addQuery(query: QueryDefinition): Requester = addQuery(query.name, query.script)

    fun addQuery(name: String, sql: String): Requester {
        addQuery(Query(name, sql, connection))
        return this
    }

    fun addQuery(queriesDirectory: URI): Requester {
        queriesDirectory.searchSqlFiles()
            .forEach {
                if (it is QueryDefinition) {
                    addQuery(it)
                }
            }
        return this
    }

    fun getQueries(): List<Query> {
        return queries.map { it.value }
    }

    fun addFunction(definition: DefinitionFunction): Requester {
        functions[definition.name] = Function(definition, connection)
        return this
    }

    fun addFunction(sql: String): Requester {
        DefinitionFunction(sql).let {
            functions[it.name] = Function(it, connection)
        }
        return this
    }

    fun addFunction(functionsDirectory: URI): Requester {
        functionsDirectory.searchSqlFiles()
            .forEach {
                if (it is DefinitionFunction) {
                    addFunction(it)
                }
            }
        return this
    }

    fun getFunction(name: String): Function {
        if (functions[name] === null) {
            throw NoFunctionDefined(name)
        }
        return functions[name]!!
    }

    fun getQuery(path: String): Query {
        if (queries[path] === null) {
            throw NoQueryDefined(path)
        }
        return queries[path]!!
    }

    class NoFunctionDefined(name: String) : Exception("No function defined for $name")
    class NoQueryDefined(path: String) : Exception("No query defined in $path")
}
