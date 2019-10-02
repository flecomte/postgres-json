package fr.postgresjson

import fr.postgresjson.entity.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityTest() {
    private class User(id: Int?): Entity<Int?>(id)
    private class ObjTest(var name: String): UuidEntityExtended<Int?, User>(User(1), User(2))

    @Test
    fun getObject() {
        val obj: ObjTest? = ObjTest("plop")
        assertTrue(obj is ObjTest)
        assertTrue(obj is UuidEntityExtended<Int?, User>)
        assertTrue(obj is EntityI)
        assertTrue(obj is Entity<Int?>)
        assertTrue(obj is Published<User>)
        assertTrue(obj is EntityCreatedBy<User>)
        assertTrue(obj is EntityUpdatedBy<User>)
        assertTrue(obj is EntityCreatedAt)
        assertTrue(obj is EntityUpdatedAt)
    }
}