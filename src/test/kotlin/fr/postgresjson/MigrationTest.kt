package fr.postgresjson

import fr.postgresjson.connexion.Requester
import fr.postgresjson.migration.Migration
import fr.postgresjson.migration.Migrations
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationTest(): TestAbstract() {
    @Test
    fun `run up query`() {
        val resources = File(this::class.java.getResource("/sql/migrations").toURI())
        val m = Migrations(resources, connection)
        m.up().apply {
            this `should contain` Pair("1", Migration.Status.OK)
            size `should be equal to` 1
        }

        m.up().size `should be equal to` 0
    }

    @Test
    fun `migration up Query should throw error if no down`() {
        val resources = File(this::class.java.getResource("/sql/migration_without_down").toURI())
        invoking {
            Migrations(resources, connection)
        } shouldThrow Migrations.DownMigrationNotDefined::class
    }

    @Test
    fun `run forced down query`() {
        val resources = File(this::class.java.getResource("/sql/migrations").toURI())
        val m = Migrations(resources, connection)
        repeat(3) {
            m.down(true).apply {
                this `should contain` Pair("1", Migration.Status.OK)
                size `should be equal to` 1
            }
        }
    }

    @Test
    fun `run dry migrations`() {
        val resources = File(this::class.java.getResource("/sql/real_migrations").toURI())
        Migrations(resources, connection).apply {
            runDry().size `should be equal to` 2
        }
        Migrations(resources, connection).apply {
            runDry().size `should be equal to` 2
        }
    }

    @Test
    fun `run dry migrations launch twice`() {
        val resources = File(this::class.java.getResource("/sql/real_migrations").toURI())
        Migrations(resources, connection).apply {
            runDry().size `should be equal to` 2
            runDry().size `should be equal to` 2
        }
    }

    @Test
    fun `run migrations`() {
        val resources = File(this::class.java.getResource("/sql/real_migrations").toURI())
        Migrations(resources, connection).apply {
            run().apply {
                size `should be equal to` 1
            }
        }
    }

    @Test
    fun `run migrations force down`() {
        val resources = File(this::class.java.getResource("/sql/real_migrations").toURI())
        val resourcesFunctions = File(this::class.java.getResource("/sql/function/Test").toURI())
        Migrations(listOf(resources, resourcesFunctions), connection).apply {
            up().apply {
                size `should be equal to` 6
            }
        }
        Migrations(listOf(resources, resourcesFunctions), connection).apply {
            forceAllDown().apply {
                size `should be equal to` 6
            }
        }
    }

    @Test
    fun `run functions migrations`() {
        val resources = File(this::class.java.getResource("/sql/function/Test").toURI())
        Migrations(resources, connection).apply {
            run().size `should be equal to` 5
        }

        val objTest: RequesterTest.ObjTest? = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function")
            .selectOne(listOf("test", "plip"))

        Assertions.assertEquals(objTest!!.id, 3)
        Assertions.assertEquals(objTest.name, "test")
    }

    @Test
    fun `run functions migrations and drop if exist`() {
        val resources = File(this::class.java.getResource("/sql/function/Test1").toURI())
        Migrations(resources, connection).apply {
            run().size `should be equal to` 1
        }

        val objTest: RequesterTest.ObjTest? = Requester(connection)
            .addFunction(resources)
            .getFunction("test_function_duplicate")
            .selectOne(listOf("test"))

        Assertions.assertEquals(objTest!!.id, 3)
        Assertions.assertEquals(objTest.name, "test")


        val resources2 = File(this::class.java.getResource("/sql/function/Test2").toURI())
        Migrations(resources2, connection).apply {
            run().size `should be equal to` 1
        }
    }
}