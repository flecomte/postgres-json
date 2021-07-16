package fr.postgresjson

import fr.postgresjson.entity.Entity
import fr.postgresjson.entity.EntityCreatedAt
import fr.postgresjson.entity.EntityCreatedBy
import fr.postgresjson.entity.EntityI
import fr.postgresjson.entity.EntityUpdatedAt
import fr.postgresjson.entity.EntityUpdatedBy
import fr.postgresjson.entity.Published
import fr.postgresjson.entity.UuidEntityExtended
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EntityTest() {
    private class User(id: UUID = UUID.randomUUID()) : Entity<UUID>(id)
    private class ObjTest(val name: String) : UuidEntityExtended<Int?, User>(User(), User())

    @Test
    fun getObject() {
        val obj = ObjTest("plop")
        assertTrue(obj is ObjTest)
        assertTrue(obj is UuidEntityExtended<Int?, User>)
        assertTrue(obj is EntityI)
        assertTrue(obj is Entity<UUID>)
        assertTrue(obj is Published<User>)
        assertTrue(obj is EntityCreatedBy<User>)
        assertTrue(obj is EntityUpdatedBy<User>)
        assertTrue(obj is EntityCreatedAt)
        assertTrue(obj is EntityUpdatedAt)
    }
}
