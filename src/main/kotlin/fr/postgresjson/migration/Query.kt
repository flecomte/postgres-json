package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.Entity
import fr.postgresjson.migration.Migration.Action
import java.io.File
import java.util.*

data class Query(
    val name: String,
    val up: String,
    val down: String,
    private val connection: Connection,
    override var executedAt: Date? = null
): Migration, Entity<String?>(name) {
    override var doExecute: Action? = null

    override fun up(): Migration.Status {
        connection.exec(up)

        File(this::class.java.getResource("/sql/migration/insertHistory.sql").toURI()).let {
            connection.selectOne<MigrationEntity>(it.readText(), listOf(name, up, down))?.let { query ->
                executedAt = query.executedAt
                doExecute = Action.OK
            }
        }

        return Migration.Status.OK
    }

    override fun down(): Migration.Status {
        connection.exec(down)

        File(this::class.java.getResource("/sql/migration/deleteHistory.sql").toURI()).let {
            connection.exec(it.readText(), listOf(name))
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
        val result = connection.inTransaction {
            up()
            down()
            it.sendQuery("ROLLBACK")
        }.join()

        return Migration.Status.OK // TODO
    }

    fun copy(): Query {
        return this.copy(name = name, up = up, down = down, connection = connection, executedAt = executedAt).also {
            it.doExecute = this.doExecute
        }
    }
}