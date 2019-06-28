package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection

class Query(
    private val up: String,
    private val down: String,
    private val connection: Connection
): Migration {
    enum class Status(i: Int) { OK(2), UP_FAIL(0), DOWN_FAIL(1) }

    override fun up(): Int {
        connection.exec(up)
        return 1
    }

    override fun down(): Int {
        connection.exec(down)
        return 1
    }

    override fun test(): Int {
        connection.inTransaction {
            connection.exec(up)
            connection.exec(down)
            it.sendQuery("ROLLBACK");
        }
        return 1
    }

    override fun status(): Int {
        val result = connection.inTransaction {
            connection.exec(up)
            connection.exec(down)
            it.sendQuery("ROLLBACK")
        }.join()

        return result.rowsAffected.toInt()
    }
}