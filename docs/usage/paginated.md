Paginated request
=================

```kotlin
import fr.postgresjson.connexion.Paginated
import fr.postgresjson.connexion.Requester
import java.util.UUID

class Article(val id: UUID, val name: String)

val request: Requester = TODO()
val article: Paginated<Article> = requester
    .getFunction("find_articles")
    .select(
        page = 1, 
        limit = 10, 
        "id" to "4a04820e-f880-4d80-b1c9-aeacccb24977"
    )
```