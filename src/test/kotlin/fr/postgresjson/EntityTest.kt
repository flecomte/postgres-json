package fr.postgresjson

import fr.postgresjson.entity.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityTest() {
    private class User(override var id: Int?): EntityI<Int?>
    private class ObjTest(var name: String): UuidEntityExtended<Int?, User>(User(1), User(2))

    @Test
    fun getObject() {
        val obj: ObjTest? = ObjTest("plop")
        assertTrue(obj is ObjTest)
        assertTrue(obj is UuidEntityExtended<Int?, User>)
        assertTrue(obj is EntityI<Int?>)
        assertTrue(obj is Entity<Int?>)
        assertTrue(obj is Published<User>)
        assertTrue(obj is CreatedBy<User>)
        assertTrue(obj is UpdatedBy<User>)
        assertTrue(obj is EntityCreatedAt)
        assertTrue(obj is EntityUpdatedAt)
    }
}