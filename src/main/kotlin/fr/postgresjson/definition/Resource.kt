package fr.postgresjson.definition

import java.io.File
import java.net.URL
import java.nio.file.Path

sealed interface Resource {
    val name: String
    val script: String
    var source: Path?

    open class ParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        fun build(file: File): Resource =
            build(file.readText(), Path.of(file.toURI()))

        fun build(url: URL): Resource =
            build(url.readText(), Path.of(url.toURI()))

        fun build(resource: String, path: Path): Resource =
            try {
                Migration(resource, path)
            } catch (e: ParseException) {
                try {
                    Function(resource, path)
                } catch (e: ParseException) {
                    try {
                        Query(resource, path)
                    } catch (e: ParseException) {
                        throw ParseException("No SQL resource found")
                    }
                }
            }
    }
}
