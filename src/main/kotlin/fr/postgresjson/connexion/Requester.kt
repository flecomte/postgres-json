package fr.postgresjson.connexion

import java.io.File
import fr.postgresjson.definition.Function as DefinitionFunction

class Requester(
    private val connection: Connection,
    private val queries: MutableMap<String, Query> = mutableMapOf(),
    private val functions: MutableMap<String, Function> = mutableMapOf()
) {
    fun addQuery(name: String, query: Query): Requester {
        queries[name] = query
        return this
    }

    fun addQuery(name: String, sql: String): Requester {
        queries[name] = Query(sql, connection)
        return this
    }

    fun addQuery(queriesDirectory: File): Requester {
        queriesDirectory.walk().filter { it.isDirectory }.forEach { directory ->
            val path = directory.name
            directory.walk().filter { it.isFile }.forEach { file ->
                val sql = file.readText()
                val fullpath = "$path/${file.nameWithoutExtension}"
                queries[fullpath] = Query(sql, connection)
            }
        }
        return this
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
        functionsDirectory.walk().filter {
            it.isDirectory
        }.forEach { directory ->
            directory.walk().filter {
                it.isFile
            }.forEach { file ->
                val fileContent = file.readText()
                addFunction(fileContent)
            }
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
        private val host: String = "localhost",
        private val port: Int = 5432,
        private val database: String = "dc-project",
        private val username: String = "dc-project",
        private val password: String = "dc-project",
        private val queriesDirectory: File? = null,
        private val functionsDirectory: File? = null
    ) {
        fun createRequester(): Requester {
            val con = Connection(host = host, port = port, database = database, username = username, password = password)
            val req = Requester(con)

            return initRequester(req)
        }

        private fun initRequester(req: Requester): Requester {
            if (queriesDirectory === null) {
                val resource = this::class.java.getResource("/sql/query")
                if (resource !== null) {
                    req.addQuery(File(resource.toURI()))
                }
            } else {
                req.addQuery(queriesDirectory)
            }

            if (functionsDirectory === null) {
                val resource = this::class.java.getResource("/sql/function")
                if (resource !== null) {
                    req.addFunction(File(resource.toURI()))
                }
            } else {
                req.addFunction(functionsDirectory)
            }

            return req
        }
    }
}
