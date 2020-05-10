package fr.postgresjson.definition

import java.nio.file.Path

class Migration(
    override val script: String,
    source: Path
) : Resource {
    override val name: String
    val direction: Direction
    override var source: Path? = null

    init {
        this.source = source
        this.direction = source.fileName.toString()
            .let {
                when {
                    it.endsWith(".down.sql") -> Direction.DOWN
                    it.endsWith(".up.sql") -> Direction.UP
                    else -> throw MigrationNotFound()
                }
            }
        this.name = source.fileName.toString()
            .substringAfterLast("/")
            .let {
                when (direction) {
                    Direction.DOWN -> it.substringBefore(".down.sql")
                    Direction.UP -> it.substringBefore(".up.sql")
                }
            }
    }

    class MigrationNotFound(cause: Throwable? = null) : Resource.ParseException("Migration not found in script", cause)
    enum class Direction { UP, DOWN }
}