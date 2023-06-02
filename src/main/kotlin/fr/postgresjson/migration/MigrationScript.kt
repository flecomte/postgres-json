package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection
import fr.postgresjson.connexion.execute
import fr.postgresjson.migration.Migration.Action
import fr.postgresjson.migration.Migration.Status
import java.util.Date

data class MigrationScript(
    val name: String,
    val up: String,
    val down: String,
    private val connection: Connection,
    override var executedAt: Date? = null
) : Migration {
    override var doExecute: Action? = null

    override fun up(): Status {
        return try {
            connection.sendQuery(up)

            this::class.java.classLoader.getResource("sql/migration/insertHistory.sql")!!.readText().let { sqlScript ->
                connection.execute<MigrationEntity>(sqlScript, listOf(name, up, down))?.let { query ->
                    executedAt = query.executedAt
                    doExecute = Action.OK
                } ?: error("No migration executed")
            }

            Status.OK
        } catch (e: Throwable) {
            Status.UP_FAIL
        }
    }

    override fun down(): Status {
        connection.sendQuery(down)

        this::class.java.classLoader.getResource("sql/migration/deleteHistory.sql")!!.readText().let {
            connection.exec(it, listOf(name))
        }

        return Status.OK
    }

    fun copy(): MigrationScript {
        return this.copy(name = name, up = up, down = down, connection = connection, executedAt = executedAt).also {
            it.doExecute = this.doExecute
        }
    }
}
