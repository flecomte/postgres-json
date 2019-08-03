package fr.postgresjson.migration

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jasync.sql.db.util.size
import fr.postgresjson.connexion.Connection
import fr.postgresjson.definition.Function.FunctionNotFound
import fr.postgresjson.entity.Entity
import fr.postgresjson.migration.Migration.Action
import fr.postgresjson.migration.Migration.Status
import fr.postgresjson.utils.LoggerDelegate
import org.slf4j.Logger
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import fr.postgresjson.definition.Function as DefinitionFunction

class MigrationEntity(
    val filename: String,
    val executedAt: Date?,
    val up: String,
    val down: String,
    val version: Int
): Entity<String?>(filename)

interface Migration {
    var executedAt: Date?
    var doExecute: Action?
    fun up(): Status
    fun down(): Status
    fun test(): Status
    fun status(): Status

    enum class Status(i: Int) { OK(2), UP_FAIL(0), DOWN_FAIL(1) }
    enum class Action { OK, UP, DOWN }
}

data class Migrations private constructor(
    private val connection: Connection,
    private val queries: MutableMap<String, Query> = mutableMapOf(),
    private val functions: MutableMap<String, Function> = mutableMapOf()
) {
    private val logger: Logger? by LoggerDelegate()
    constructor(directory: File, connection: Connection): this(listOf(directory), connection)

    constructor(directories: List<File>, connection: Connection): this(connection) {
        initDB()
        getMigrationFromDB()
        getMigrationFromDirectory(directories)
        queries.forEach { (_, query) ->
            if (query.doExecute === null) {
                query.doExecute = Action.DOWN
            }
        }

        functions.forEach { (_, function) ->
            if (function.doExecute === null) {
                function.doExecute = Action.DOWN
            }
        }
    }

    private var initialized = false

    /**
     * Get all migration from DB
     */
    private fun getMigrationFromDB() {
        this::class.java.classLoader.getResource("sql/migration/findAllFunction.sql")!!.readText().let {
            connection.select<MigrationEntity>(it, object: TypeReference<List<MigrationEntity>>() {})
                .map { function ->
                    functions[function.filename] = Function(function.up, function.down, connection, function.executedAt)
                }
        }

        this::class.java.classLoader.getResource("sql/migration/findAllHistory.sql")!!.readText().let {
            connection.select<MigrationEntity>(it, object: TypeReference<List<MigrationEntity>>() {})
                .map { query ->
                    queries[query.filename] = Query(query.filename, query.up, query.down, connection, query.executedAt)
                }
        }
    }

    /**
     * Get all migration from multiples Directories
     */
    private fun getMigrationFromDirectory(directory: List<File>) {
        directory.forEach {
            getMigrationFromDirectory(it)
        }
    }

    /**
     * Get all migration from Directory
     */
    private fun getMigrationFromDirectory(directory: File) {
        directory.walk().filter {
            it.isFile
        }.forEach { file ->
            if (file.name.endsWith(".up.sql")) {
                file.path.substring(0, file.path.size - 7).let {
                    try {
                        val down = File("$it.down.sql").readText()
                        val up = file.readText()
                        val name = file.name.substring(0, file.name.size - 7)
                        addQuery(name, up, down)
                    } catch (e: FileNotFoundException) {
                        throw DownMigrationNotDefined("$it.down.sql", e)
                    }
                }
            } else if (file.name.endsWith(".down.sql")) {
                // Nothing
            } else {
                val fileContent = file.readText()
                try {
                    addFunction(fileContent)
                } catch(e: FunctionNotFound) {
                    // Nothing
                }
            }
        }
    }

    enum class Direction { UP, DOWN }

    internal class DownMigrationNotDefined(path: String, cause: FileNotFoundException):
        Throwable("The file $path whas not found", cause)

    fun addFunction(newDefinition: DefinitionFunction, callback: (Function) -> Unit = {}): Migrations {
        val currentFunction = functions[newDefinition.name]
        if (currentFunction === null || currentFunction `is different from` newDefinition) {
            functions[newDefinition.name] = Function(newDefinition, newDefinition, connection).apply {
                doExecute = Action.UP
            }
        } else {
            functions[newDefinition.name]?.doExecute = Action.OK
        }

        callback(functions[newDefinition.name]!!)

        return this
    }

    fun addFunction(sql: String): Migrations {
        addFunction(DefinitionFunction(sql))
        return this
    }

    fun addQuery(name: String, up: String, down: String, callback: (Query) -> Unit = {}): Migrations {
        if (queries[name] === null) {
            queries[name] = Query(name, up, down, connection).apply {
                doExecute = Action.UP
            }
        } else {
            queries[name]!!.apply {
                doExecute = Action.OK
            }
        }

        callback(queries[name]!!)

        return this
    }

    private fun initDB() {
        if (!initialized) {
            this::class.java.classLoader.getResource("sql/migration/createHistoryShema.sql")!!.readText().let {
                connection.sendQuery(it)
            }
            this::class.java.classLoader.getResource("sql/migration/createFunctionShema.sql")!!.readText().let {
                connection.sendQuery(it)
            }
            initialized = true
        }
    }

    private fun lock() {
        this::class.java.classLoader.getResource("sql/migration/lockMigrationTables.sql")!!.readText().let {
            connection.sendQuery(it)
        }
    }

    internal fun up(): Map<String, Status> {
        val list: MutableMap<String, Status> = mutableMapOf()
        queries.forEach {
            it.value.let { query ->
                if (query.doExecute == Action.UP) {
                    query.up().let { status ->
                        list[query.name] = status
                    }
                }
            }
        }

        functions.forEach {
            it.value.let { function ->
                if (function.doExecute == Action.UP) {
                    function.up().let { status ->
                        list[function.name] = status
                    }
                }
            }
        }

        return list.toMap()
    }

    internal fun down(force: Boolean = false): Map<String, Status> {
        val list: MutableMap<String, Status> = mutableMapOf()
        queries.forEach {
            it.value.let { query ->
                if (query.doExecute == Action.DOWN || force) {
                    query.down().let { status ->
                        list[query.name] = status
                    }
                }
            }
        }

        functions.forEach {
            it.value.let { function ->
                if (function.doExecute == Action.DOWN || force) {
                    function.down().let { status ->
                        list[function.name] = status
                    }
                }
            }
        }

        return list.toMap()
    }

    fun run(): Map<Pair<String, Direction>, Status> {
        val list: MutableMap<Pair<String, Direction>, Status> = mutableMapOf()
        logger?.info("Migration Begin")
        connection.apply {
            sendQuery("BEGIN")
            lock()
            up().map {
                list[Pair(it.key, Direction.UP)] = it.value
            }
            down().map {
                list[Pair(it.key, Direction.DOWN)] = it.value
            }
            sendQuery("COMMIT")
        }
        logger?.info("Migration done")

        return list.toMap()
    }

    fun runDry(): Map<Pair<String, Direction>, Status> {
        return this.copy().runTest()
    }

    fun forceAllDown(): Map<Pair<String, Direction>, Status> {
        val list: MutableMap<Pair<String, Direction>, Status> = mutableMapOf()
        logger?.info("Migration DOWN begin")
        connection.apply {
            sendQuery("BEGIN")
            lock()
            down(true).map {
                list[Pair(it.key, Direction.DOWN)] = it.value
            }
            sendQuery("COMMIT")
        }
        logger?.info("Migration DOWN done")

        return list.toMap()
    }

    private fun runTest(): Map<Pair<String, Direction>, Status> {
        val list: MutableMap<Pair<String, Direction>, Status> = mutableMapOf()
        connection.apply {
            sendQuery("BEGIN")
            up().map {
                list[Pair(it.key, Direction.UP)] = it.value
            }
            down(true).map {
                list[Pair(it.key, Direction.DOWN)] = it.value
            }
            sendQuery("ROLLBACK")
        }

        return list.toMap()
    }

    fun copy(): Migrations {
        val queriesCopy = queries.map {
            it.key to it.value.copy()
        }.toMap().toMutableMap()

        val functionsCopy = functions.map {
            it.key to it.value.copy()
        }.toMap().toMutableMap()

        return Migrations(connection, queriesCopy, functionsCopy)
    }

    fun status(): Map<String, Int> {
        TODO("not implemented")
    }
}