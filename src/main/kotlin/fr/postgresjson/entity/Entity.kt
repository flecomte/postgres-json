package fr.postgresjson.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.joda.time.DateTime
import java.util.*
import kotlin.reflect.KClass

/* ID */
interface EntityI<T> {
    var id: T?
    val className: KClass<EntityI<T?>>
        @JsonIgnore() get() = this::class as KClass<EntityI<T?>>
}

abstract class Entity<T>(override var id: T? = null): EntityI<T?>
abstract class UuidEntity(override var id: UUID? = UUID.randomUUID()): Entity<UUID?>(id)
abstract class IdEntity(override var id: Int? = null): Entity<Int?>(id)

/* Version */
interface EntityVersioning<T> {
    var version: T
}

interface EntityVersioningIncrement: EntityVersioning<Int?>
class EntityVersioningIncrementImp: EntityVersioningIncrement {
    override var version: Int? = null
}

interface EntityVersioningDate: EntityVersioning<DateTime?>
class EntityVersioningDateImp: EntityVersioningDate {
    override var version: DateTime? = null
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
interface CreatedBy<T: EntityI<*>> {
    var createdBy: T?
}

interface UpdatedBy<T: EntityI<*>> {
    var updatedBy: T?
}

class EntityCreatedByImp<UserT: EntityI<*>>: CreatedBy<UserT> {
    override var createdBy: UserT? = null
}

class EntityUpdatedByImp<UserT: EntityI<*>>: UpdatedBy<UserT> {
    override var updatedBy: UserT? = null
}

/* Published */
interface Published<UserT: EntityI<*>> {
    var publishedAt: DateTime?
    var publishedBy: UserT?
}

class EntityPublishedImp<UserT: EntityI<*>>: Published<UserT> {
    override var publishedAt: DateTime? = null
    override var publishedBy: UserT? = null
}

/* Implementation */
abstract class EntityImp<T, UserT: EntityI<*>>: Entity<T>(),
    EntityCreatedAt by EntityCreatedAtImp(),
    EntityUpdatedAt by EntityUpdatedAtImp(),
    CreatedBy<UserT> by EntityCreatedByImp(),
    UpdatedBy<UserT> by EntityUpdatedByImp()

abstract class EntityExtended<T, UserT: EntityI<*>>:
    EntityImp<T, UserT>(),
    EntityVersioningIncrement by EntityVersioningIncrementImp(),
    Published<UserT> by EntityPublishedImp()

