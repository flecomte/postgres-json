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

    init {
        directory.walk().filter {
            it.isDirectory
        }.forEach { directory ->
            directory.walk().filter {
                it.isFile
            }.forEach { file ->
                if (file.name.endsWith(".up.sql")) {
                    val up = file.readText()
                    val down = file.path.substring(0, file.path.size - 7).let {
                        try {
                            File("$it.down.sql").readText()
                        } catch (e: FileNotFoundException) {
                            throw DownMigrationNotDefined("$it.down.sql", e)
                        }
                    }
                    addQuery(up, down)
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
        DefinitionFunction.build(sql).forEach {
            functions[it.name] = Function(it, it, connection)
        }
        return this
    }

    fun addQuery(up: String, down: String): Migrations {
        queries.add(Query(up, down, connection))
        return this
    }

    override fun up(): Int {
        var count = 0
        queries.forEach {
            it.up()
            ++count
        }

        return count
    }

    override fun down(): Int {
        var count = 0
        queries.forEach {
            it.down()
            ++count
        }

        return count
    }

    override fun test(): Int {
        var count = 0
        connection.inTransaction {
            count += up()
            count += down()
            it.sendQuery("ROLLBACK");
        }.join()

        return count
    }

    override fun status(): Int {
        TODO("not implemented")
    }
}