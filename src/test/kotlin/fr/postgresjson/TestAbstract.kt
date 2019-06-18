package fr.postgresjson

import fr.postgresjson.connexion.Connection
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.io.File

@TestInstance(PER_CLASS)
abstract class TestAbstract {
    protected fun getConnextion(): Connection {
        return Connection(database = "test", username = "test", password = "test")
    }

    @BeforeAll
    fun beforeAll() {
        val initSQL = File(this::class.java.getResource("/fixtures/init.sql").toURI())
        val promise = getConnextion().connect().sendQuery(initSQL.readText())
        promise.join()
    }

    @AfterAll
    fun afterAll() {
        val downSQL = File(this::class.java.getResource("/fixtures/down.sql").toURI())
        getConnextion().connect().sendQuery(downSQL.readText()).join()
    }
}