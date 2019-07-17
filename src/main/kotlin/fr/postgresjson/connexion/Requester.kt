package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.entity.EntityI
import java.io.File
import java.util.concurrent.CompletableFuture
import fr.postgresjson.definition.Function as DefinitionFunction

class Requester (
    private val connection: Connection,
    private val queries: MutableMap<String, Query> = mutableMapOf(),
    private val functions: MutableMap<String, Function> = mutableMapOf())
{
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

    class Query(private val sql: String, override val connection : Connection): Executable {
        override fun toString(): String {
            return sql
        }

        override fun <R : EntityI<*>> select(typeReference: TypeReference<R>, values: List<Any?>): R? {
            return connection.select(this.toString(), typeReference, values)
        }
        inline fun <reified R : EntityI<*>> selectOne(values: List<Any?> = emptyList()): R? = select(object: TypeReference<R>() {}, values)

        override fun <R: EntityI<*>> select(typeReference: TypeReference<R>, values: Map<String, Any?>): R? {
            return connection.select(this.toString(), typeReference, values)
        }
        inline fun <reified R : EntityI<*>> selectOne(values: Map<String, Any?>): R? = select(object: TypeReference<R>() {}, values)

        override fun <R : EntityI<*>> select(typeReference: TypeReference<List<R>>, values: List<Any?>): List<R> {
            return connection.select(this.toString(), typeReference, values)
        }
        inline fun <reified R : EntityI<*>> select(values: List<Any?> = emptyList()): List<R> = select(object: TypeReference<List<R>>() {}, values)

        override fun <R: EntityI<*>> select(typeReference: TypeReference<List<R>>, values: Map<String, Any?>): List<R> {
            return connection.select(this.toString(), typeReference, values)
        }
        inline fun <reified R : EntityI<*>> select(values: Map<String, Any?>): List<R> = select(object: TypeReference<List<R>>() {}, values)

        override fun exec(values: List<Any?>): CompletableFuture<QueryResult> {
            return connection.exec(sql, values)
        }

        override fun exec(values: Map<String, Any?>): CompletableFuture<QueryResult> {
            return connection.exec(sql, values)
        }
    }

    class Function(val definition: DefinitionFunction, override val connection : Connection): Executable {
        override fun toString(): String {
            return definition.name
        }

        /**
         * Select One entity with list of parameters
         */
        override fun <R : EntityI<*>> select(typeReference: TypeReference<R>, values: List<Any?>): R? {
            val args = compileArgs(values)
            val sql = "SELECT * FROM ${definition.name} ($args)"

            return connection.select(sql, typeReference, values)
        }
        inline fun <reified R: EntityI<*>> selectOne(values: List<Any?> = emptyList()): R? = select(object: TypeReference<R>() {}, values)

        /**
         * Select One entity with named parameters
         */
        override fun <R : EntityI<*>> select(typeReference: TypeReference<R>, values: Map<String, Any?>): R? {
            val args = compileArgs(values)
            val sql = "SELECT * FROM ${definition.name} ($args)"

            return connection.select(sql, typeReference, values)
        }
        inline fun <reified R: EntityI<*>> selectOne(values: Map<String, Any?>): R? = select(object: TypeReference<R>() {}, values)

        /**
         * Select list of entities with list of parameters
         */
        override fun <R : EntityI<*>> select(typeReference: TypeReference<List<R>>, values: List<Any?>): List<R> {
            val args = compileArgs(values)
            val sql = "SELECT * FROM ${definition.name} ($args)"

            return connection.select(sql, typeReference, values)
        }
        inline fun <reified R: EntityI<*>> select(values: List<Any?> = emptyList()): List<R> = select(object: TypeReference<List<R>>() {}, values)

        /**
         * Select list of entities with named parameters
         */
        override fun <R: EntityI<*>> select(typeReference: TypeReference<List<R>>, values: Map<String, Any?>): List<R> {
            val args = compileArgs(values)
            val sql = "SELECT * FROM ${definition.name} ($args)"

            return connection.select(sql, typeReference, values)
        }
        inline fun <reified R: EntityI<*>> select(values: Map<String, Any?>): List<R> = select(object: TypeReference<List<R>>() {}, values)

        override fun exec(values: List<Any?>): CompletableFuture<QueryResult> {
            val args = compileArgs(values)
            val sql = "SELECT * FROM ${definition.name} ($args)"

            return connection.exec(sql, values)
        }

        override fun exec(values: Map<String, Any?>): CompletableFuture<QueryResult> {
            val args = compileArgs(values)
            val sql = "SELECT * FROM ${definition.name} ($args)"

            return connection.exec(sql, values)
        }

        private fun compileArgs(values: List<Any?>): String {
            val placeholders = values
                .filterIndexed { index, any ->
                    definition.parameters[index].default === null || any !== null
                }
                .mapIndexed { index, any ->
                    "?::" + definition.parameters[index].type
                }

            return placeholders.joinToString(separator=", ")
        }

        private fun compileArgs(values: Map<String, Any?>): String {
            val parameters = definition.getParametersIndexedByName()
            val placeholders = values
                .filter { entry ->
                    val parameter = parameters[entry.key] ?: error("Parameter ${entry.key} not exist")
                    parameter.default === null || entry.value !== null
                }
                .map { entry ->
                    val parameter = parameters[entry.key]!!
                    "${parameter.name} := :${parameter.name}::" + parameter.type
                }

            return placeholders.joinToString(separator=", ")
        }
    }

    interface Executable {
        val connection : Connection
        override fun toString(): String

        fun <R : EntityI<*>> select(typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R?
        fun <R : EntityI<*>> select(typeReference: TypeReference<R>, values: Map<String, Any?>): R?

        fun <R : EntityI<*>> select(typeReference: TypeReference<List<R>>, values: List<Any?> = emptyList()): List<R>
        fun <R : EntityI<*>> select(typeReference: TypeReference<List<R>>, values: Map<String, Any?>): List<R>

        fun exec(values: List<Any?> = emptyList()): CompletableFuture<QueryResult>
        fun exec(values: Map<String, Any?>): CompletableFuture<QueryResult>
    }

    class RequesterFactory(
        private val host: String = "localhost",
        private val port: Int = 5432,
        private val database: String = "dc-project",
        private val username: String = "dc-project",
        private val password: String = "dc-project",
        private val queriesDirectory: File? = null,
        private val functionsDirectory: File? = null
    )
    {
        fun createRequester(): Requester
        {
            val con = Connection(host = host, port = port, database = database, username = username, password = password)
            val req = Requester(con)

            return initRequester(req)
        }

        private fun initRequester(req: Requester): Requester
        {
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
