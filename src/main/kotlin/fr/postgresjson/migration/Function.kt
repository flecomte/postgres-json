package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection
import fr.postgresjson.migration.Migration.Action
import fr.postgresjson.migration.Migration.Status
import java.io.File
import java.util.*
import fr.postgresjson.definition.Function as DefinitionFunction

class Function(
    val up: DefinitionFunction,
    val down: DefinitionFunction,
    private val connection: Connection,
    override var executedAt: Date? = null
): Migration {
    val name = up.name
    override var doExecute: Action? = null

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

    override fun up(): Status {
        connection.exec(up.script)

        File(this::class.java.getResource("/sql/migration/insertFunction.sql").toURI()).let {
            connection.selectOne<String, MigrationEntity?>(it.readText(), listOf(up))?.let { function ->
                executedAt = function.executedAt
                doExecute = Action.OK
            }
        }
        return Status.OK
    }

    override fun down(): Status {
        connection.exec(down.script)

        File(this::class.java.getResource("/sql/migration/deleteFunction.sql").toURI()).let {
            connection.exec(it.readText(), listOf(down))
        }
        return Status.OK
    }

    override fun test(): Status {
        connection.inTransaction {
            up()
            down()
            it.sendQuery("ROLLBACK");
        }.join()

        return Status.OK // TODO
    }

    override fun status(): Status {
        val result = connection.inTransaction {
            up()
            down()
            it.sendQuery("ROLLBACK")
        }.join()

        return Status.OK // TODO
    }
}