package fr.postgresjson.connexion

import java.io.File
import fr.postgresjson.definition.Function as DefinitionFunction

class Requester(
    private val connection: Connection,
    private val queries: MutableMap<String, Query> = mutableMapOf(),
    private val functions: MutableMap<String, Function> = mutableMapOf()
) {
    fun addQuery(query: Query): Requester {
        queries[query.name] = query
        return this
    }

    fun addQuery(name: String, sql: String): Requester {
        addQuery(Query(name, sql, connection))
        return this
    }

    fun addQuery(queriesDirectory: File): Requester {
        queriesDirectory.walk()
            .filter { it.isFile && it.extension == "sql" }
            .forEach {
                val path = it.parentFile.nameWithoutExtension
                addQuery("$path/${it.nameWithoutExtension}", it.readText())
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

    fun addFunction(functionsDirectory: File): Requester {
        functionsDirectory.walk()
            .filter { it.isFile && it.extension == "sql" }
            .forEach {
                addFunction(it.readText())
            }
        return this
    }

    fun getFunction(name: String): Function {
        if (functions[name] === null) {
            throw Exception("No function defined for $name")
        }
        return functions[name]!!
    }

    fun getQuery(path: String): Query {
        if (queries[path] === null) {
            throw Exception("No query defined in $path")
        }
        return queries[path]!!
    }

    class RequesterFactory(
        private val connection: Connection,
        private val queriesDirectory: File? = null,
        private val functionsDirectory: File? = null
    ) {
        constructor(
            host: String = "localhost",
            port: Int = 5432,
            database: String = "dc-project",
            username: String = "dc-project",
            password: String = "dc-project",
            queriesDirectory: File? = null,
            functionsDirectory: File? = null
        ): this(
            Connection(host = host, port = port, database = database, username = username, password = password),
            queriesDirectory,
            functionsDirectory
        )

        fun createRequester(): Requester {
            return initRequester(Requester(connection))
        }

        private fun initRequester(req: Requester): Requester {
            if (queriesDirectory !== null) {
                req.addQuery(queriesDirectory)
            }

            if (functionsDirectory !== null) {
                req.addFunction(functionsDirectory)
            }

            return req
        }
    }
}
