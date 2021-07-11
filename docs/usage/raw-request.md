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
    val name: String
): Serializable

val result: Inventor? = connection.selectOne(
    """
    SELECT json_build_object(
        'id', '9e65de49-712e-47ce-8bf2-dfffae53a82e', 
        'name', :name
    )
    """,
    mapOf("name" to "Nikola Tesla")
)

val inventor = connection.selectOne<Inventor>("SELECT * FROM mytable WHERE id = :id")

val inventors: List<Inventor> = connection.select("SELECT * FROM mytable WHERE status = 'done'")
```


See [ConnectionTest.kt](/src/test/kotlin/fr/postgresjson/ConnectionTest.kt) for more examples.