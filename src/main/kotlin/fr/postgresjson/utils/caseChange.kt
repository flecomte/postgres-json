package fr.postgresjson.utils

fun String.toCamelCase(): String {
    return "_[a-zA-Z]".toRegex().replace(this) {
        it.value.replace("_", "").uppercase()
    }
}
