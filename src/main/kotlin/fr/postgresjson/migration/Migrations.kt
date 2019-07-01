package fr.postgresjson.migration

import com.github.jasync.sql.db.util.size
import fr.postgresjson.connexion.Connection
import java.io.File
import java.io.FileNotFoundException
import fr.postgresjson.definition.Function as DefinitionFunction


interface Migration {
    fun up(): Int
    fun down(): Int
    fun test(): Int
    fun status(): Int
}

class Migrations(directory: File, private val connection: Connection): Migration {
    private val queries: MutableList<Query> = mutableListOf()
    private val functions: MutableMap<String, Function> = mutableMapOf()
    private var initialized = false

    init {
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
                            addQuery(file.name, up, down)
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

    class DownMigrationNotDefined(path: String, cause: FileNotFoundException): Throwable("The file $path whas not found", cause)

    fun addFunction(definition: DefinitionFunction): Migrations {
        functions[definition.name] = Function(definition, definition, connection)
        return this
    }

    fun addFunction(sql: String): Migrations {
        DefinitionFunction(sql).let {
            functions[it.name] = Function(it, it, connection)
        }
        return this
    }

    fun addQuery(name: String, up: String, down: String): Migrations {
        queries.add(Query(name, up, down, connection))
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

    override fun up(): Int {
        initDB()
        var count = 0
        queries.forEach {
            it.up()
            ++count
        }

        return count
    }

    override fun down(): Int {
        initDB()
        var count = 0
        queries.forEach {
            it.down()
            ++count
        }

        return count
    }

    override fun test(): Int {
        initDB()
        var count = 0
        connection.inTransaction {
            count += up()
            count += down()
            it.sendQuery("ROLLBACK");
        }.join()

        return count
    }

    override fun status(): Int {
        initDB()
        TODO("not implemented")
    }
}