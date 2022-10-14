package fr.postgresjson.utils

import fr.postgresjson.definition.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

fun URL.searchSqlFiles() = this.toURI().searchSqlFiles()

fun URI.searchSqlFiles() = sequence {
    val logger: Logger = LoggerFactory.getLogger("sqlFilesSearch")
    val uri: URI = this@searchSqlFiles
    logger.debug("""SQL files found in "${uri.toString().substringAfter('!')}" :""")
    if (uri.scheme == "jar") {
        try {
            FileSystems.getFileSystem(uri)
        } catch (e: FileSystemNotFoundException) {
            FileSystems.newFileSystem(uri, emptyMap<String, Any>())
        }

        uri
            .walk(5)
            .asSequence()
            .filter { it.fileName.toString().endsWith(".sql") }
            .map { it.toUri().toURL() }
            .forEach {
                logger.debug(it.toString())
                yield(Resource.build(it))
            }
    } else {
        uri
            .walk(5)
            .asSequence()
            .map { it.toFile() }
            .filter { it.isFile && it.extension == "sql" }
            .forEach {
                logger.debug(it.toString())
                yield(Resource.build(it))
            }
    }
}

private fun Path.walk(maxDepth: Int = 2147483647, vararg options: FileVisitOption) = Files.walk(this, maxDepth, *options).sorted()
private fun URI.walk(maxDepth: Int = 2147483647, vararg options: FileVisitOption) = Files.walk(Path.of(this), maxDepth, *options).sorted()
