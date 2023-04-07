package fr.postgresjson

import fr.postgresjson.connexion.Connection
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import java.io.File

open class SqlFixtureListener : BeforeSpecListener, AfterSpecListener {
    private val connection = Connection(database = "json_test", username = "test", password = "test", port = 35555)

    override suspend fun beforeSpec(spec: Spec) {
        val initSQL = File(this::class.java.getResource("/fixtures/init.sql")!!.toURI())
        connection
            .connect()
            .sendQuery(initSQL.readText())
            .join()
    }

    override suspend fun afterSpec(spec: Spec) {
        val downSQL = File(this::class.java.getResource("/fixtures/down.sql")!!.toURI())
        connection
            .apply { connect().sendQuery(downSQL.readText()).join() }
            .disconnect()
    }
}
