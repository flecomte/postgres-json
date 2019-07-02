package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection
import java.util.*
import fr.postgresjson.definition.Function as DefinitionFunction

class Function(
    val up: DefinitionFunction,
    val down: DefinitionFunction,
    private val connection: Connection,
    override var executedAt: Date? = null
): Migration {
    val name = up.name

    init {
        if (up.name !== down.name) {
            throw Exception("UP and DOWN migration must be the same")
        }
    }

    constructor(
        up: String,
        down: String,
        connection: Connection,
        executedAt: Date? = null):
    this(
        DefinitionFunction(up),
        DefinitionFunction(down),
        connection,
        executedAt
    )

    override fun up(): Migration.Status {
        connection.exec(up.script)
        // TODO insert to migration Table
        return Migration.Status.OK
    }

    override fun down(): Migration.Status {
        connection.exec(down.script)
        // TODO insert to migration Table
        return Migration.Status.OK
    }

    override fun test(): Migration.Status {
        connection.inTransaction {
            up()
            down()
            it.sendQuery("ROLLBACK");
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