package fr.postgresjson.definition

import java.nio.file.Path

class Query(
    override val script: String,
    override var source: Path
) : Resource {
    override val name: String = getNameFromComment(script) ?: getNameFromFile(source)

    /** Try to get name from comment in file */
    private fun getNameFromComment(script: String): String? =
        """-- *name ?: ?(?<name>[^ \n]+)"""
            .toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
            .find(script)?.let {
                it.groups["name"]?.value?.trim()
            }

    /** Try to get name from the filename */
    private fun getNameFromFile(source: Path): String = source
        .fileName.toString()
        .substringAfterLast("/")
        .substringBeforeLast(".sql")
}
