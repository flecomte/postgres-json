package fr.postgresjson.migration

import fr.postgresjson.connexion.Connection
import fr.postgresjson.migration.Migration.Action
import fr.postgresjson.migration.Migration.Status
import java.util.*
import fr.postgresjson.definition.Function as DefinitionFunction

data class Function(
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
        executedAt: Date? = null
    ): this(
        DefinitionFunction(up),
        DefinitionFunction(down),
        connection,
        executedAt
    )

    override fun up(): Status {
        connection.sendQuery(up.script)

        this::class.java.classLoader.getResource("sql/migration/insertFunction.sql")!!.readText().let {
            connection.selectOne<MigrationEntity>(it, listOf(up.name, up.getDefinition(), up.script, down.script))?.let { function ->
                executedAt = function.executedAt
                doExecute = Action.OK
            }
        }
        return Status.OK
    }

    override fun down(): Status {
        connection.sendQuery(down.script)

        this::class.java.classLoader.getResource("sql/migration/deleteFunction.sql")!!.readText().let {
            connection.exec(it, listOf(down))
        }
        return Status.OK
    }

    override fun test(): Status {
        connection.inTransaction {
            up()
            down()
            it.sendQuery("ROLLBACK")
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

    fun copy(): Function {
        return this.copy(up = up, down = down, connection = connection, executedAt = executedAt).also {
            it.doExecute = this.doExecute
        }
    }
}