package fr.postgresjson

import fr.postgresjson.entity.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityTest() {
    private class ObjTest(var name: String): EntityExtended<Int?, Int?>()

    @Test
    fun getObject() {
        val obj: ObjTest? = ObjTest("plop")
        assertTrue(obj is ObjTest)
        assertTrue(obj is EntityExtended<Int?, Int?>)
        assertTrue(obj is EntityI<Int?>)
        assertTrue(obj is Entity<Int?>)
        assertTrue(obj is Published<Int?>)
        assertTrue(obj is CreatedBy<Int?>)
        assertTrue(obj is UpdatedBy<Int?>)
        assertTrue(obj is EntityCreatedAt)
        assertTrue(obj is EntityUpdatedAt)
    }
}