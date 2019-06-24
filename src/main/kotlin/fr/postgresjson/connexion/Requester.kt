package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.QueryResult
import fr.postgresjson.entity.EntityI
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.text.RegexOption.IGNORE_CASE
import kotlin.text.RegexOption.MULTILINE

class Requester (
    private val connection: Connection,
    queries: List<Query> = listOf(),
    functions: List<Function> = listOf())
{
    private val queries = mutableMapOf<String, Query>()
    private val functions = mutableMapOf<String, Function>()

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

    fun addFunction(function: Function): Requester {
        functions[function.name] = function
        return this
    }

    fun addFunction(sql: String): Requester {
        getDefinitions(sql).forEach {
            functions[it.name] = it
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

    private fun getDefinitions(functionContent: String): List<Function> {
        val functionRegex = """create .*(procedure|function) *(?<name>[^(\s]+)\s*\((?<params>(\s*((IN|OUT|INOUT|VARIADIC)?\s+)?([^\s,)]+\s+)?([^\s,)]+)(\s+(?:default\s|=)\s*[^\s,)]+)?\s*(,|(?=\))))*)\) *(?<return>RETURNS *[^ ]+)?"""
            .toRegex(setOf(IGNORE_CASE, MULTILINE))

        val paramsRegex = """\s*(?<param>((?<direction>IN|OUT|INOUT|VARIADIC)?\s+)?(?<name>[^\s,)]+\s+)?(?<type>[^\s,)]+)(\s+(?<default>default\s|=)\s*[^\s,)]+)?)\s*(,|$)"""
            .toRegex(setOf(IGNORE_CASE, MULTILINE))

        return functionRegex.findAll(functionContent).map { queryMatch ->
            val functionName = queryMatch.groups["name"]?.value?.trim()
            val functionParameters = queryMatch.groups["params"]?.value?.trim()
            val returns = queryMatch.groups["return"]?.value?.trim()

            /* Create parameters definition */
            val parameters = if (functionParameters !== null) {
                val matchesParams = paramsRegex.findAll(functionParameters)
                matchesParams.map { paramsMatch ->
                    Function.Parameter(
                        paramsMatch.groups["name"]!!.value.trim(),
                        paramsMatch.groups["type"]!!.value.trim(),
                        paramsMatch.groups["direction"]?.value?.trim(),
                        paramsMatch.groups["default"]?.value?.trim())
                }.toList()
            } else {
                listOf()
            }

            Function(functionName!!, parameters, connection)
        }.toList()
    }

    fun getFunction(name: String): Function {
        if (functions[name] === null) {
            throw Exception("No function defined for $name")
        }
        return functions[name]!!
    }

    fun getQuery(path: String): Query {
        if (queries[path] === null) {
            throw Exception("No query defined for $path")
        }
        return queries[path]!!
    }

    class Query(private val sql: String, private val connection : Connection) {
        override fun toString(): String {
            return sql
        }

        fun <T, R : EntityI<T?>?> selectOne(typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R? {
            return connection.selectOne(this.toString(), typeReference, values)
        }

        inline fun <T, reified R : EntityI<T?>?> selectOne(values: List<Any?> = emptyList()): R? = selectOne(object: TypeReference<R>() {}, values)

        fun <T, R : List<EntityI<T?>?>> select(typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R? {
            return connection.select(this.toString(), typeReference, values)
        }

        inline fun <T, reified R : List<EntityI<T?>?>> select(values: List<Any?> = emptyList()): R? = select(object: TypeReference<R>() {}, values)

        fun exec(values: List<Any?> = emptyList()): CompletableFuture<QueryResult> {
            return connection.exec(sql, values)
        }
    }

    class Function(val name: String, val parameters: List<Parameter>, private val connection : Connection) {

        class Parameter(val name: String, val type: String, direction: Direction? = Direction.IN, val default: Any? = null)
        {
            val direction: Direction

            init {
                if (direction === null) {
                    this.direction = Direction.IN
                } else {
                    this.direction = direction
                }
            }
            constructor(name: String, type: String, direction: String? = "IN", default: Any? = null) : this(
                name = name,
                type = type,
                direction = direction?.let { Direction.valueOf(direction.toUpperCase())},
                default = default
            )
            enum class Direction { IN, OUT, INOUT }
        }

        override fun toString(): String {
            return name
        }

        fun <T, R : EntityI<T?>?> selectOne(typeReference: TypeReference<R>, values: List<String?> = emptyList()): R? {
            val args = compileArgs(values)
            val sql = "SELECT * FROM $name ($args)"

            return connection.selectOne(sql, typeReference, values)
        }

        inline fun <T, reified R: EntityI<T?>?> selectOne(values: List<String?> = emptyList()): R? = selectOne(object: TypeReference<R>() {}, values)

        fun <T, R : List<EntityI<T?>?>> select(typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R? {
            val args = compileArgs(values)
            val sql = "SELECT * FROM $name ($args)"

            return connection.select(sql, typeReference, values)
        }

        inline fun <T, reified R: List<EntityI<T?>?>> select(values: List<Any?> = emptyList()): R? = select(object: TypeReference<R>() {}, values)

        fun exec(values: List<Any?> = emptyList()): CompletableFuture<QueryResult> {
            val args = compileArgs(values)
            val sql = "SELECT * FROM $name ($args)"

            return connection.exec(sql, values)
        }

        private fun compileArgs(values: List<Any?>): String {
            val placeholders = values
                .filterIndexed { index, any ->
                    this.parameters[index].default === null || any !== null
                }
                .mapIndexed { index, any ->
                    "?::" + this.parameters[index].type
                }

            return placeholders.joinToString(separator=", ")
        }
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
