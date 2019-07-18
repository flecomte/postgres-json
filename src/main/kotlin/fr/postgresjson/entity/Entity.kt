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
    fun isValid(): Boolean
}

/* Author */
interface CreatedBy<T> {
    var createdBy: User<T>?
}

interface UpdatedBy<T> {
    var updatedBy: User<T>?
}

class EntityCreatedByImp<T>: CreatedBy<T> {
    override var createdBy: User<T>? = null
}

class EntityUpdatedByImp<T>: UpdatedBy<T> {
    override var updatedBy: User<T>? = null
}

/* Published */
interface Published<UserT> {
    var publishedAt: DateTime?
    var publishedBy: User<UserT>?
}

class EntityPublishedImp<UserT>: Published<UserT> {
    override var publishedAt: DateTime? = null
    override var publishedBy: User<UserT>? = null
}

/* Implementation */
abstract class EntityImp<T, UserT>: Entity<T>(),
    EntityCreatedAt by EntityCreatedAtImp(),
    EntityUpdatedAt by EntityUpdatedAtImp(),
    CreatedBy<UserT> by EntityCreatedByImp(),
    UpdatedBy<UserT> by EntityUpdatedByImp()

abstract class EntityExtended<T, UserT>:
    EntityImp<T, UserT>(),
    EntityVersioningIncrement by EntityVersioningIncrementImp(),
    Published<UserT> by EntityPublishedImp()

