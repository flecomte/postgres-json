package fr.postgresjson.migration

import com.fasterxml.jackson.core.type.TypeReference
import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.Entity
import fr.postgresjson.migration.Migration.Action
import fr.postgresjson.migration.Migration.Status
import fr.postgresjson.utils.LoggerDelegate
import fr.postgresjson.utils.searchSqlFiles
import org.slf4j.Logger
import java.io.FileNotFoundException
import java.net.URI
import java.util.Date
import fr.postgresjson.definition.Function as DefinitionFunction
import fr.postgresjson.definition.Migration as DefinitionMigration

class MigrationEntity(
    val filename: String,
    val executedAt: Date?,
    val up: String,
    val down: String,
    val version: Int
) : Entity<String?>(filename)

interface Migration {
    var executedAt: Date?
    var doExecute: Action?
    fun up(): Status
    fun down(): Status
    fun test(): Status

    enum class Status(val i: Int) { OK(2), UP_FAIL(0), DOWN_FAIL(1) }
    enum class Action { OK, UP, DOWN }
}

class Migrations private constructor(
    private val connection: Connection,
    private val migrationsScripts: MutableMap<String, MigrationScript> = mutableMapOf(),
    private val functions: MutableMap<String, Function> = mutableMapOf()
) {
    private var directories: List<URI> = emptyList()
    private val logger: Logger? by LoggerDelegate()
    constructor(directory: URI, connection: Connection) : this(listOf(directory), connection)
    constructor(connection: Connection, vararg directory: URI) : this(directory.toList(), connection)

    constructor(directories: List<URI>, connection: Connection) : this(connection) {
        initDB()
        this.directories = directories
        reset()
    }

    private fun reset() {
        migrationsScripts.clear()
        functions.clear()

        getMigrationFromDB()
        getMigrationFromDirectory(directories)

        migrationsScripts.forEach { (_, query) ->
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
            connection.select(it, object : TypeReference<List<MigrationEntity>>() {})
                .map { function ->
                    functions[function.filename] = Function(function.up, function.down, connection, function.executedAt)
                }
        }

        this::class.java.classLoader.getResource("sql/migration/findAllHistory.sql")!!.readText().let {
            connection.select(it, object : TypeReference<List<MigrationEntity>>() {})
                .map { query ->
                    migrationsScripts[query.filename] = MigrationScript(query.filename, query.up, query.down, connection, query.executedAt)
                }
        }
    }

    /**
     * Get all migration from multiples Directories
     */
    private fun getMigrationFromDirectory(directory: List<URI>) {
        directory.forEach {
            getMigrationFromDirectory(it)
        }
    }

    /**
     * Get all migration from Directory
     */
    private fun getMigrationFromDirectory(directory: URI) {
        val downs: MutableMap<String, DefinitionMigration> = mutableMapOf()

        directory.searchSqlFiles().apply {
            /* Set Down Migration */
            forEach { migration ->
                if (migration is DefinitionMigration && migration.direction == DefinitionMigration.Direction.DOWN) {
                    downs += migration.name to migration
                }
            }

            /* Set up migrations and functions */
            forEach { migration ->
                if (migration is DefinitionMigration && migration.direction == DefinitionMigration.Direction.UP) {
                    val down = downs[migration.name] ?: throw DownMigrationNotDefined(migration.name + ".down.sql")
                    downs -= migration.name

                    addMigrationScript(migration, down)
                } else if (migration is DefinitionFunction) {
                    addFunction(migration)
                }
            }
        }
    }

    enum class Direction { UP, DOWN }

    internal class DownMigrationNotDefined(path: String, cause: FileNotFoundException? = null) :
        Throwable("The file $path whas not found", cause)

    fun addFunction(newDefinition: DefinitionFunction, callback: (Function) -> Unit = {}): Migrations {
        val currentFunction = functions[newDefinition.name]
        if (currentFunction === null || currentFunction `is different from` newDefinition) {
            val oldDefinition = functions[newDefinition.name]?.up ?: newDefinition
            functions[newDefinition.name] = Function(newDefinition, oldDefinition, connection).apply {
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

    fun addMigrationScript(up: DefinitionMigration, down: DefinitionMigration, callback: (MigrationScript) -> Unit = {}): Migrations =
        addMigrationScript(up.name, up.script, down.script, callback)

    fun addMigrationScript(name: String, up: String, down: String, callback: (MigrationScript) -> Unit = {}): Migrations {
        if (migrationsScripts[name] === null) {
            migrationsScripts[name] = MigrationScript(name, up, down, connection).apply {
                doExecute = Action.UP
            }
        } else {
            migrationsScripts[name]!!.apply {
                doExecute = Action.OK
            }
        }

        callback(migrationsScripts[name]!!)

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
        migrationsScripts.forEach {
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
        migrationsScripts.forEach {
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
        reset()

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
        reset()

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

    private fun copy(): Migrations {
        val queriesCopy = migrationsScripts.map {
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
