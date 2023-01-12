package fr.postgresjson.functionGenerator

import java.net.URI
import org.junit.jupiter.api.Test

class FunctionGeneratorTest {

    @Test
    fun generate() {
        val functionDirectory = this::class.java.getResource("/sql/function/Test")!!.toURI()
        FunctionGenerator(functionDirectory)
            .generate(URI( "./src/test/kotlin/fr/postgresjson/functionGenerator/generated/"))
    }
}