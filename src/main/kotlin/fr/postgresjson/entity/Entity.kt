package fr.postgresjson.entity

import org.joda.time.DateTime
import java.util.*

interface Serializable
interface EntityI : Serializable
interface Parameter : Serializable

abstract class Entity<T>(open var id: T? = null) : EntityI
open class UuidEntity(override var id: UUID? = UUID.randomUUID()) : Entity<UUID>(id)
open class IdEntity(override var id: Int? = null) : Entity<Int>(id)

/* Version */
interface EntityVersioning<ID, NUMBER> {
    var versionId: ID
    var versionNumber: NUMBER?
}

class UuidEntityVersioning : EntityVersioning<UUID, Int> {
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

interface EntityDeletedAt {
    var deletedAt: DateTime?
    fun isDeleted(): Boolean {
        val deletedAt = deletedAt
        return deletedAt != null && deletedAt < DateTime.now()
    }
}

class EntityCreatedAtImp : EntityCreatedAt {
    override var createdAt: DateTime? = null
}

class EntityUpdatedAtImp : EntityUpdatedAt {
    override var updatedAt: DateTime? = null
}

class EntityDeletedAtImp : EntityDeletedAt {
    override var deletedAt: DateTime? = null
}

/* Author */
interface EntityCreatedBy<T : EntityI> {
    var createdBy: T?
}

interface EntityUpdatedBy<T : EntityI> {
    var updatedBy: T?
}

interface EntityDeletedBy<T : EntityI> {
    var deletedBy: T?
}

class EntityCreatedByImp<UserT : EntityI>(
    override var createdBy: UserT?
) : EntityCreatedBy<UserT>

class EntityUpdatedByImp<UserT : EntityI>(
    override var updatedBy: UserT?
) : EntityUpdatedBy<UserT>

class EntityDeletedByImp<UserT : EntityI>(
    override var deletedBy: UserT?
) : EntityDeletedBy<UserT>

/* Mixed */
class EntityDeletedImp<UserT : EntityI>(
    override var deletedBy: UserT? = null
) : EntityDeletedBy<UserT>,
    EntityDeletedAt by EntityDeletedAtImp()

class EntityUpdatedImp<UserT : EntityI>(
    override var updatedAt: DateTime? = null,
    override var updatedBy: UserT? = null
) : EntityUpdatedBy<UserT>,
    EntityUpdatedAt by EntityUpdatedAtImp()

class EntityCreatedImp<UserT : EntityI>(
    override var createdAt: DateTime? = null,
    override var createdBy: UserT? = null
) : EntityCreatedBy<UserT>,
    EntityCreatedAt by EntityCreatedAtImp()

/* Published */
interface Published<UserT : EntityI> {
    var publishedAt: DateTime?
    var publishedBy: UserT?
}

class EntityPublishedImp<UserT : EntityI>(
    override var publishedBy: UserT?
) : Published<UserT> {
    override var publishedAt: DateTime? = null
}

/* Implementation */
abstract class EntityImp<T, UserT : EntityI>(
    updatedBy: UserT?
) : Entity<T>(),
    EntityCreatedAt by EntityCreatedAtImp(),
    EntityUpdatedAt by EntityUpdatedAtImp(),
    EntityDeletedAt by EntityDeletedAtImp(),
    EntityCreatedBy<UserT> by EntityCreatedByImp(updatedBy),
    EntityUpdatedBy<UserT> by EntityUpdatedByImp(updatedBy),
    EntityDeletedBy<UserT> by EntityDeletedByImp(updatedBy)

abstract class UuidEntityExtended<T, UserT : EntityI>(
    updatedBy: UserT?,
    publishedBy: UserT?
) :
    EntityImp<T, UserT>(updatedBy),
    EntityVersioning<UUID, Int> by UuidEntityVersioning(),
    Published<UserT> by EntityPublishedImp(publishedBy)
