# Stored Procedure
*Execute stored procedure with requester*

You can execute a stored procedure (previously defined in a migration) via the Requester

To do that:

1. First, instantiate the requester
```kotlin
import fr.postgresjson.connexion.Requester
import fr.postgresjson.connexion.Connection

val connection: Connection = TODO()

val requester = Requester(
    connection = connection,
    functionsDirectory = this::class.java.getResource("/sql/functions")?.toURI() ?: error("No sql function found")
).createRequester()
```

2. then, define Entities
```kotlin
import java.util.UUID
import org.joda.time.DateTime

enum class Roles { ROLE_USER, ROLE_ADMIN }

class User(
    id: UUID = UUID.randomUUID(),
    override var username: String,
    var blockedAt: DateTime? = null,
    var roles: List<Roles> = emptyList()
)

@SqlSerializable
class UserForCreate(
    id: UUID = UUID.randomUUID(),
    username: String,
    val password: String,
    blockedAt: DateTime? = null,
    roles: List<Roles> = emptyList()
)
```
3. and, define Repositories
[See SQL function](../migrations/migrations.md)

```kotlin
import fr.postgresjson.connexion.Requester
import fr.postgresjson.repository.RepositoryI
import java.util.UUID

class UserRepository(override var requester: Requester): RepositoryI {
    fun findById(id: UUID): User {
        return requester
            .getFunction("find_user_by_id") // Use the name of the function
            .execute("id" to id) // You can pass parameters by their names. The underscore prefix on parameters is not required to be mapped. 
            // Throw exception if user not found
    }

    fun insert(user: UserForCreate): User {
        return requester
            .getFunction("insert_user")
            .execute("resource" to user)
    }
}
```

4. And at last, execute queries
```kotlin
import fr.postgresjson.connexion.Requester
import java.util.UUID

val requester: Requester = TODO()
val userRepo = UserRepository(requester)

val user: User = userRepo.findById(UUID.fromString(id))

val newUser: UserForCreate = TODO()
val userInserted: User = userRepo.insert(newUser)
```