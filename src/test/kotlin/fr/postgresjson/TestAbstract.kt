package fr.postgresjson

import fr.postgresjson.connexion.Connection
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.io.File

@TestInstance(PER_CLASS)
abstract class TestAbstract {
    private var connection = Connection(database = "test", username = "test", password = "test")

    protected fun getConnextion(): Connection {
        return connection
    }

    @BeforeEach
    fun beforeAll() {
        val initSQL = File(this::class.java.getResource("/fixtures/init.sql").toURI())
        getConnextion().connect().createStatement().executeUpdate(initSQL.readText())
    }

    @AfterEach
    fun afterAll() {
        val downSQL = File(this::class.java.getResource("/fixtures/down.sql").toURI())
        getConnextion().connect().createStatement().executeUpdate(downSQL.readText())
        getConnextion().connect().close()
    }
}