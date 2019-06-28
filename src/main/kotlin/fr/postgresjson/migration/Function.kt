package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection
import fr.postgresjson.definition.Function as DefinitionFunction

class Function(
    private val up: DefinitionFunction,
    private val down: DefinitionFunction,
    private val connection: Connection
): Migration {
    enum class Status(i: Int) { OK(2), UP_FAIL(0), DOWN_FAIL(1) }

    override fun up(): Int {
        connection.exec(up.script)
        return 1
    }

    override fun down(): Int {
        connection.exec(down.script)
        return 1
    }

    override fun test(): Int {
        connection.inTransaction {
            connection.exec(up.script)
            connection.exec(down.script)
            it.sendQuery("ROLLBACK");
        }
        return 1
    }

    override fun status(): Int {
        val result = connection.inTransaction {
            connection.exec(up.script)
            connection.exec(down.script)
            it.sendQuery("ROLLBACK")
        }.join()

        return result.rowsAffected.toInt()
    }
}