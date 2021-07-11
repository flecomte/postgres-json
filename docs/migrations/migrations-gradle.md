# Execute Migrations with Gradle

You can execute migration with a Gradle task like this:

```kotlin
// build.gradle.kts
import fr.postgresjson.connexion.Connection
import fr.postgresjson.connexion.Requester
import fr.postgresjson.migration.Migrations

buildscript {
    dependencies {
        classpath("com.github.flecomte:postgres-json:+")
    }
}

val migration by tasks.registering {
    doLast {
        val connection = Connection(
            host = "localhost",
            port = 5432,
            database = "database",
            username = "username",
            password = "password"
        )
        Migrations(
            connection,
            file("$buildDir/resources/main/sql/migrations").toURI(),
            file("$buildDir/resources/main/sql/functions").toURI()
        ).run()
    }
}
```

```shell
$ gradle migration
```