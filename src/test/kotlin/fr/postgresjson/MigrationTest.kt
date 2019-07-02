package fr.postgresjson

import fr.postgresjson.migration.Migration
import fr.postgresjson.migration.Migrations
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationTest(): TestAbstract() {
    @Test
    fun upQuery() {
        val resources = File(this::class.java.getResource("/sql/migrations").toURI())
        val m = Migrations(resources, getConnextion())
        m.up() `should contain` Pair("1", Migration.Status.OK)
        m.up().size `should be equal to` 1
    }

    @Test
    fun `migration up Query should throw error if no down`() {
        val resources = File(this::class.java.getResource("/sql/migration_without_down").toURI())
        invoking {
            Migrations(resources, getConnextion())
        } shouldThrow Migrations.DownMigrationNotDefined::class
    }

    @Test
    fun downQuery() {
        val resources = File(this::class.java.getResource("/sql/migrations").toURI())
        val m = Migrations(resources, getConnextion())
        m.down() `should contain` Pair("1", Migration.Status.OK)
        m.down().size `should be equal to` 1
    }

    @Test
    fun `test up and down migrations`() {
        val resources = File(this::class.java.getResource("/sql/real_migrations").toURI())
        val m = Migrations(resources, getConnextion())
        m.test().size `should be equal to` 2
        m.test().size `should be equal to` 2
    }
}