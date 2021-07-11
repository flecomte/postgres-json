# Init connection

Before execute any query you must instantiate the connection.
```kotlin
import fr.postgresjson.connexion.Connection

val connection = Connection(
    host = "localhost",
    port = 5432,
    database = "mydb",
    username = "john",
    password = "azerty"
)
```