package fr.postgresjson.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.joda.time.DateTime
import java.util.*
import kotlin.reflect.KClass

/* ID */
interface EntityI<T> {
    var id: T?
    val className: KClass<EntityI<T>>
        @JsonIgnore() get() = this::class as KClass<EntityI<T>>
}

abstract class Entity<T>(override var id: T? = null): EntityI<T>
open class UuidEntity(override var id: UUID? = UUID.randomUUID()): Entity<UUID>(id)
open class IdEntity(override var id: Int? = null): Entity<Int>(id)

/* Version */
interface EntityVersioning<ID, NUMBER> {
    var versionId: ID
    var versionNumber: NUMBER?
}

class UuidEntityVersioning: EntityVersioning<UUID, Int> {
    override var versionId: UUID = UUID.randomUUID()
    override var versionNumber: Int? = null
}

/* Dates */
interface EntityCreatedAt {
    var createdAt: DateTime?
}

interface EntityUpdatedAt {
    var updatedAt: DateTime?
}

class EntityCreatedAtImp: EntityCreatedAt {
    override var createdAt: DateTime? = null
}

class EntityUpdatedAtImp: EntityUpdatedAt {
    override var updatedAt: DateTime? = null
}

/* Author */
interface EntityCreatedBy<T: EntityI<*>> {
    var createdBy: T?
}

interface EntityUpdatedBy<T: EntityI<*>> {
    var updatedBy: T?
}

class EntityCreatedByImp<UserT: EntityI<*>>(
    override var createdBy: UserT?
): EntityCreatedBy<UserT>

class EntityUpdatedByImp<UserT: EntityI<*>>(
    override var updatedBy: UserT?
): EntityUpdatedBy<UserT>

/* Published */
interface Published<UserT: EntityI<*>> {
    var publishedAt: DateTime?
    var publishedBy: UserT?
}

class EntityPublishedImp<UserT: EntityI<*>>(
    override var publishedBy: UserT?
): Published<UserT> {
    override var publishedAt: DateTime? = null
}

/* Implementation */
abstract class EntityImp<T, UserT: EntityI<*>>(
    updatedBy: UserT?
): Entity<T>(),
    EntityCreatedAt by EntityCreatedAtImp(),
    EntityUpdatedAt by EntityUpdatedAtImp(),
    EntityCreatedBy<UserT> by EntityCreatedByImp(updatedBy),
    EntityUpdatedBy<UserT> by EntityUpdatedByImp(updatedBy)

abstract class UuidEntityExtended<T, UserT: EntityI<*>>(
    updatedBy: UserT?,
    publishedBy: UserT?
):
    EntityImp<T, UserT>(updatedBy),
    EntityVersioning<UUID, Int> by UuidEntityVersioning(),
    Published<UserT> by EntityPublishedImp(publishedBy)
