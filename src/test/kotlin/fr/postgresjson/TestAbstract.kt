package fr.postgresjson

import fr.postgresjson.connexion.Connection
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.io.File

@TestInstance(PER_CLASS)
abstract class TestAbstract {
    protected val connection = Connection(database = "json_test", username = "test", password = "test", port = 5555)

    @BeforeEach
    fun beforeAll() {
        val initSQL = File(this::class.java.getResource("/fixtures/init.sql").toURI())
        connection
            .connect()
            .sendQuery(initSQL.readText())
            .join()
    }

    @AfterEach
    fun afterAll() {
        val downSQL = File(this::class.java.getResource("/fixtures/down.sql").toURI())
        connection.connect().apply {
            sendQuery(downSQL.readText()).join()
        }.disconnect()
    }
}
