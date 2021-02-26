package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.Entity
import fr.postgresjson.migration.Migration.Action
import java.util.Date

data class MigrationScript(
    val name: String,
    val up: String,
    val down: String,
    private val connection: Connection,
    override var executedAt: Date? = null
) : Migration, Entity<String?>(name) {
    override var doExecute: Action? = null

    override fun up(): Migration.Status {
        connection.sendQuery(up)

        this::class.java.classLoader.getResource("sql/migration/insertHistory.sql")!!.readText().let {
            connection.selectOne<MigrationEntity>(it, listOf(name, up, down))?.let { query ->
                executedAt = query.executedAt
                doExecute = Action.OK
            }
        }

        return Migration.Status.OK
    }

    override fun down(): Migration.Status {
        connection.sendQuery(down)

        this::class.java.classLoader.getResource("sql/migration/deleteHistory.sql")!!.readText().let {
            connection.exec(it, listOf(name))
        }

        return Migration.Status.OK
    }

    override fun test(): Migration.Status {
        connection.inTransaction {
            up()
            down()
            it.sendQuery("ROLLBACK")
        }.join()

        return Migration.Status.OK // TODO
    }

    override fun status(): Migration.Status {
        connection.inTransaction {
            up()
            down()
            it.sendQuery("ROLLBACK")
        }.join()

        return Migration.Status.OK // TODO
    }

    fun copy(): MigrationScript {
        return this.copy(name = name, up = up, down = down, connection = connection, executedAt = executedAt).also {
            it.doExecute = this.doExecute
        }
    }
}
