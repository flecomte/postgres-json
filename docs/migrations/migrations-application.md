# Execute migration in application
```kotlin
import fr.postgresjson.migration.Migrations
import fr.postgresjson.connexion.Connection

val conn: Connection = TODO()
val migrations = Migrations(
    conn,
    this::class.java.getResource("/sql/migrations")?.toURI() ?: error("No migrations found"),
    this::class.java.getResource("/sql/functions")?.toURI() ?: error("No sql function found")
)

migrations.status() // Show executed and not executed migrations
migrations.runDry() // Execute migration in transaction and rollback at the end
migrations.run() // Execute migration in transaction and commit if no error
```