# Raw request
You can execute query directly from the code like this:
(*see [Init connection](./init-connection.md) before*)

```kotlin
import fr.postgresjson.connexion.Connection

val connection: Connection = TODO()

val result: QueryResult = connection.exec(
    "SELECT id FROM inventor WHERE name = :name",
    mapOf("name" to "Nikola Tesla")
)
val id: String = result.rows[0].getString(0)
```

And if you must map the query result with an entity, you can do it like this:
```kotlin
import java.util.UUID
import fr.postgresjson.entity.Serializable
import fr.postgresjson.connexion.Connection

val connection: Connection = TODO()

data class Inventor(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val roles: List<String> = listOf(),
)

// Select one entity
val result: Inventor = connection.execute(
    """
    SELECT json_build_object(
        'id', '9e65de49-712e-47ce-8bf2-dfffae53a82e', 
        'name', :name
    )
    """,
    mapOf("name" to "Nikola Tesla")
)

// Select multiple entities
val result = connection.execute<List<Inventor>>(
    """
    SELECT json_build_array(
        json_build_object(
           'id', '9e65de49-712e-47ce-8bf2-dfffae53a82e',
           'name', :name
        ),
        json_build_object(
           'id', '32f67ed3-af6d-403b-a3b9-5fe3540c3412',
           'name', :name2
        )
    )
    """,
    mapOf(
        "name" to "Nikola Tesla",
        "name2" to "Albert Einstein",
    )
)

// Select multiple with real query
val result: List<Inventor> = connection.execute(
    """
        select json_agg(i)
        from inventor i
        where roles @> ARRAY[:role];
    """,
    mapOf("role" to "ADMIN")
)


// Select multiple with only some rows
val result: List<Inventor> = connection.execute(
    """
        select json_agg(i)
        from (
             select id, name
             from inventor
        ) i;
    """
)
```


See [ConnectionTest.kt](/src/test/kotlin/fr/postgresjson/ConnectionTest.kt) for more examples.