package fr.postgresjson.connexion

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import fr.postgresjson.Serializer
import fr.postgresjson.entity.EntityI
import java.io.File
import kotlin.text.RegexOption.IGNORE_CASE
import kotlin.text.RegexOption.MULTILINE

class Connection(
    private val host: String = "localhost",
    private val port: Int = 5432,
    private val database: String = "dc-project",
    private val username: String = "dc-project",
    private val password: String = "dc-project",
    queriesDirectory: File? = null,
    functionsDirectory: File? = null
) {
    private val queries = mutableMapOf<String, Query>()
    private val functions = mutableMapOf<String, Function>()
    private lateinit var connection: ConnectionPool<PostgreSQLConnection>
    private val serializer = Serializer()

    init {
        if (queriesDirectory === null) {
            val resource = this::class.java.getResource("/sql/query")
            if (resource !== null) {
                fetchQueries(File(resource.toURI()))
            }
        } else {
            fetchQueries(queriesDirectory)
        }

        if (functionsDirectory === null) {
            val resource = this::class.java.getResource("/sql/function")
            if (resource !== null) {
                fetchFunctions(File(resource.toURI()))
            }
        } else {
            fetchFunctions(functionsDirectory)
        }
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
    }

    class Function(val name: String, val parameters: List<Parameter>, private val connection : Connection) {

        class Parameter(val name: String, val type: String, direction: Direction? = Direction.IN)
        {
            val direction: Direction

            init {
                if (direction === null) {
                    this.direction = Direction.IN
                } else {
                    this.direction = direction
                }
            }
            constructor(name: String, type: String, direction: String? = "IN") : this(
                name = name,
                type = type,
                direction = direction?.let { Direction.valueOf(direction.toUpperCase())}
            )
            enum class Direction { IN, OUT, INOUT }
        }

        override fun toString(): String {
            return name
        }

        fun <T, R : EntityI<T?>?> selectOne(typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R? {
            val args = values.joinToString()
            val sql = "SELECT * FROM $name ($args)"

            return connection.selectOne(sql, typeReference, values)
        }

        inline fun <T, reified R: EntityI<T?>?> selectOne(values: List<Any?> = emptyList()): R? = selectOne(object: TypeReference<R>() {}, values)

        fun <T, R : List<EntityI<T?>?>> select(typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R? {
            val args = values.joinToString()
            val sql = "SELECT * FROM $name ($args)"

            return connection.select(sql, typeReference, values)
        }

        inline fun <T, reified R: List<EntityI<T?>?>> select(values: List<Any?> = emptyList()): R? = select(object: TypeReference<R>() {}, values)
    }

    fun connect(): ConnectionPool<PostgreSQLConnection> {
        if (!::connection.isInitialized || !connection.isConnected()) {
            connection = PostgreSQLConnectionBuilder.createConnectionPool(
                "jdbc:postgresql://$host:$port/$database?user=$username&password=$password"
            )
        }
        return connection
    }

    fun <T, R : EntityI<T?>?> selectOne(sql: String, typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R? {
        val future = connect().sendPreparedStatement(sql, values)
        val json = future.get().rows[0].getString(0)
        return if (json === null) {
            null
        } else {
            serializer.deserialize<T, R>(json, typeReference)
        }
    }

    inline fun <T, reified R : EntityI<T?>?> selectOne(sql: String, values: List<Any?> = emptyList()): R? = selectOne(sql, object: TypeReference<R>() {}, values)

    fun <T, R : List<EntityI<T?>?>> select(sql: String, typeReference: TypeReference<R>, values: List<Any?> = emptyList()): R {
        val future = connect().sendPreparedStatement(sql, values)
        val json = future.get().rows[0].getString(0)
        return if (json === null) {
            listOf<EntityI<T?>?>() as R
        } else {
            serializer.deserializeList(json, typeReference)
        }
    }

    inline fun <T, reified R : List<EntityI<T?>?>> select(sql: String, values: List<Any?> = emptyList()): R = select(sql, object : TypeReference<R>() {}, values)

    private fun fetchQueries(queriesDirectory: File) {
        queriesDirectory.walk().filter { it.isDirectory }.forEach { directory ->
            val path = directory.name
            directory.walk().filter { it.isFile }.forEach { file ->
                val sql = file.readText()
                val fullpath = "$path/${file.nameWithoutExtension}"
                queries[fullpath] = Query(sql, this)
            }
        }
    }

    private fun fetchFunctions(functionsDirectory: File) {
        functionsDirectory.walk().filter { it.isDirectory }.forEach { directory ->
            directory.walk().filter { it.isFile }.forEach { file ->
                val fileContent = file.readText()
                getDefinitions(fileContent).forEach {
                    functions[it.name] = it
                }
            }
        }
    }

    private fun getDefinitions(functionContent: String): List<Function>
    {
        val functionRegex = """create .*(procedure|function) *(?<name>[^(\s]+)\s*\((?<params>(\s*((IN|OUT|INOUT|VARIADIC)?\s+)?([^\s,)]+\s+)?([^\s,)]+)(\s+(?:default\s|=)\s*[^\s,)]+)?\s*(,|(?=\))))*)\) *(?<return>RETURNS *[^ ]+)?"""
            .toRegex(setOf(IGNORE_CASE, MULTILINE))

        val paramsRegex = """\s*(?<param>((?<direction>IN|OUT|INOUT|VARIADIC)?\s+)?(?<name>[^\s,)]+\s+)?(?<type>[^\s,)]+)(\s+(?:default\s|=)\s*[^\s,)]+)?)\s*(,|$)"""
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
                        paramsMatch.groups["direction"]?.value?.trim())
                }.toList()
            } else {
                listOf()
            }

            Function(functionName!!, parameters, this)
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
}