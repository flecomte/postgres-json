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
interface User<T>: EntityI<T> {
}

/* Author */
interface CreatedBy<T: User<*>> {
    var createdBy: T?
}

interface UpdatedBy<T: User<*>> {
    var updatedBy: T?
}

class EntityCreatedByImp<UserT: User<*>>: CreatedBy<UserT> {
    override var createdBy: UserT? = null
}

class EntityUpdatedByImp<UserT: User<*>>: UpdatedBy<UserT> {
    override var updatedBy: UserT? = null
}

/* Published */
interface Published<UserT: User<*>> {
    var publishedAt: DateTime?
    var publishedBy: UserT?
}

class EntityPublishedImp<UserT: User<*>>: Published<UserT> {
    override var publishedAt: DateTime? = null
    override var publishedBy: UserT? = null
}

/* Implementation */
abstract class EntityImp<T, UserT: User<*>>: Entity<T>(),
    EntityCreatedAt by EntityCreatedAtImp(),
    EntityUpdatedAt by EntityUpdatedAtImp(),
    CreatedBy<UserT> by EntityCreatedByImp(),
    UpdatedBy<UserT> by EntityUpdatedByImp()

abstract class EntityExtended<T, UserT: User<*>>:
    EntityImp<T, UserT>(),
    EntityVersioningIncrement by EntityVersioningIncrementImp(),
    Published<UserT> by EntityPublishedImp()

