package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection
import fr.postgresjson.definition.Function as DefinitionFunction

class Function(
    val up: DefinitionFunction,
    val down: DefinitionFunction,
    private val connection: Connection
): Migration {
    val name = up.name
    enum class Status(i: Int) { OK(2), UP_FAIL(0), DOWN_FAIL(1) }

    init {
        if (up.name !== down.name) {
            throw Exception("UP and DOWN migration must be the same")
        }
    }

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