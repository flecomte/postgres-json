package fr.postgresjson.definition

import java.io.File

interface Resource {
    val name: String
    val script: String
    var source: File?
}

interface ResourceCollection {
    val parameters: List<Parameter>
}