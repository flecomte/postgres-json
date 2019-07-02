package fr.postgresjson.migration

import com.github.jasync.sql.db.util.size
import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.Entity
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import fr.postgresjson.definition.Function as DefinitionFunction

class MigrationEntity(
    val filename: String,
    val definition: String,
    val executedAt: Date,
    val up: String,
    val down: String,
    val version: Int
): Entity<String?>(filename)

interface Migration {
    var executedAt: Date?
    fun up(): Status
    fun down(): Status
    fun test(): Status
    fun status(): Status
    fun doExecute(): Boolean

    enum class Status(i: Int) { OK(2), UP_FAIL(0), DOWN_FAIL(1) }
}

class Migrations(directory: File, private val connection: Connection) {
    private val queries: MutableMap<String, Query> = mutableMapOf()
    private val functions: MutableMap<String, Function> = mutableMapOf()
    private var initialized = false

    init {
        initDB()
        getMigrationFromDB()
        getMigrationFromDirectory(directory)
    }

    /**
     * Get all migration from DB
     */
    private fun getMigrationFromDB() {
        File(this::class.java.getResource("/sql/migration/findAllFunction.sql").toURI()).let {
            connection.select<String, List<MigrationEntity?>>(it.readText())
                .filterNotNull().map { function ->
                    functions[function.filename] = Function(function.up, function.down, connection, function.executedAt)
                }
        }

        File(this::class.java.getResource("/sql/migration/findAllHistory.sql").toURI()).let {
            connection.select<String, List<MigrationEntity?>>(it.readText())
                .filterNotNull().map { query ->
                    queries[query.filename] = Query(query.filename, query.up, query.down, connection, query.executedAt)
                }
        }
    }

    /**
     * Get all migration from Directory
     */
    private fun getMigrationFromDirectory(directory: File) {
        directory.walk().filter {
            it.isDirectory
        }.forEach { directory ->
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
                    addFunction(fileContent)
                }
            }
        }
    }

    enum class Direction { UP, DOWN }
    class DownMigrationNotDefined(path: String, cause: FileNotFoundException): Throwable("The file $path whas not found", cause)

    fun addFunction(definition: DefinitionFunction): Migrations {
        if (functions[definition.name] === null) {
            functions[definition.name] = Function(definition, definition, connection)
        }
        return this
    }

    fun addFunction(sql: String): Migrations {
        addFunction(DefinitionFunction(sql))
        return this
    }

    fun addQuery(name: String, up: String, down: String): Migrations {
        if (queries[name] === null) {
            queries[name] = Query(name, up, down, connection)
        }
        return this
    }

    private fun initDB() {
        if (!initialized) {
            File(this::class.java.getResource("/sql/migration/createHistoryShema.sql").toURI()).let {
                connection.connect().sendQuery(it.readText()).join()
            }
            File(this::class.java.getResource("/sql/migration/createFunctionShema.sql").toURI()).let {
                connection.connect().sendQuery(it.readText()).join()
            }
            initialized = true
        }
    }

    fun up(): Map<String, Migration.Status> {
        val list: MutableMap<String, Migration.Status> = mutableMapOf()
        queries.forEach {
            it.value.let { query ->
                if (query.doExecute()) {
                    query.up().let { status ->
                        list[query.name] = status
                    }
                }
            }
        }

        functions.forEach {
            it.value.let { function ->
                if (function.doExecute()) {
                    function.up().let { status ->
                        list[function.name] = status
                    }
                }
            }
        }

        return list.toMap()
    }

    fun down(): Map<String, Migration.Status> {
        val list: MutableMap<String, Migration.Status> = mutableMapOf()
        queries.forEach {
            it.value.let { query ->
                if (query.doExecute()) {
                    query.down().let { status ->
                        list[query.name] = status
                    }
                }
            }
        }

        functions.forEach {
            it.value.let { function ->
                if (function.doExecute()) {
                    function.down().let { status ->
                        list[function.name] = status
                    }
                }
            }
        }

        return list.toMap()
    }

    fun test(): Map<Pair<String, Direction>, Migration.Status> {
        var list: MutableMap<Pair<String, Direction>, Migration.Status> = mutableMapOf()
        connection.connect().let {
            it.sendQuery("BEGIN").join()
            up().map {
                list.set(Pair(it.key, Direction.UP), it.value)
            }
            down().map {
                list.set(Pair(it.key, Direction.DOWN), it.value)
            }
            it.sendQuery("ROLLBACK").join()
        }

        return list.toMap()
    }

    fun status(): Map<String, Int> {
        TODO("not implemented")
    }
}