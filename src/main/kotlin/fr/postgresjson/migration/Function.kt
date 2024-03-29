package fr.postgresjson.migration

import com.github.jasync.sql.db.postgresql.exceptions.GenericDatabaseException
import fr.postgresjson.connexion.Connection
import fr.postgresjson.connexion.selectOne
import fr.postgresjson.migration.Migration.Action
import fr.postgresjson.migration.Migration.Status
import java.util.Date
import java.util.concurrent.CompletionException
import fr.postgresjson.definition.Function as DefinitionFunction

data class Function(
    val up: DefinitionFunction,
    val down: DefinitionFunction,
    private val connection: Connection,
    override var executedAt: Date? = null
) : Migration {
    val name = up.name
    override var doExecute: Action? = null

    init {
        if (up.name != down.name) {
            throw Exception("UP and DOWN migration must have the same name [${up.name} != ${down.name}]")
        }
    }

    constructor(
        up: String,
        down: String,
        connection: Connection,
        executedAt: Date? = null
    ) : this(
        DefinitionFunction(up),
        DefinitionFunction(down),
        connection,
        executedAt
    )

    override fun up(): Status {
        return try {
            try {
                connection.sendQuery(up.script)
            } catch (e: CompletionException) {
                val cause = e.cause
                if (cause is GenericDatabaseException && cause.errorMessage.fields['C'] == "42P13") {
                    connection.sendQuery("drop function ${down.getDefinition()}")
                    connection.sendQuery(up.script)
                }
            }

            this::class.java.classLoader
                .getResource("sql/migration/insertFunction.sql")!!.readText()
                .let { connection.selectOne<MigrationEntity>(it, listOf(up.name, up.getDefinition(), up.script, down.script)) }
                ?.let { function ->
                    executedAt = function.executedAt
                    doExecute = Action.OK
                }

            Status.OK
        } catch (e: Throwable) {
            Status.UP_FAIL
        }
    }

    override fun down(): Status {
        return try {
            connection.sendQuery(down.script)

            this::class.java.classLoader
                .getResource("sql/migration/deleteFunction.sql")!!
                .readText()
                .let { connection.sendQuery(it, listOf(down.name)) }

            Status.OK
        } catch (e: Throwable) {
            Status.DOWN_FAIL
        }
    }

    override fun test(): Status {
        connection.inTransaction {
            up()
            down()
            sendQuery("ROLLBACK")
        }

        return Status.OK
    }

    fun copy(): Function = this
        .copy(up = up, down = down, connection = connection, executedAt = executedAt)
        .also { it.doExecute = this.doExecute }

    infix fun `is different from`(other: DefinitionFunction): Boolean = other.script != this.up.script
}
