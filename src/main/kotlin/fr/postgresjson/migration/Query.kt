package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection
import fr.postgresjson.entity.Entity
import java.util.*

class Query(
    val name: String,
    val up: String,
    val down: String,
    private val connection: Connection,
    override var executedAt: Date? = null
): Migration, Entity<String?>(name) {
    override fun up(): Migration.Status {
        connection.exec(up).join()
        // TODO insert to migration Table

        return Migration.Status.OK
    }

    override fun down(): Migration.Status {
        connection.exec(down).join()
        // TODO insert to migration Table

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

    override fun doExecute(): Boolean {
        return executedAt === null
    }
}