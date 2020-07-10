package fr.postgresjson.entity.mutable

import fr.postgresjson.entity.EntityI
import org.joda.time.DateTime
import java.util.*

interface EntityRefI<T> : EntityI {
    var id: T?
}

interface UuidEntityI : EntityRefI<UUID> {
    override var id: UUID?
}

interface IdEntityI : EntityRefI<Int> {
    override var id: Int?
}

abstract class Entity<T>(override var id: T? = null) : EntityRefI<T>
open class UuidEntity(id: UUID? = null) : UuidEntityI, Entity<UUID>(id ?: UUID.randomUUID())
open class IdEntity(override var id: Int? = null) : IdEntityI, Entity<Int>(id)

/* Version */
interface EntityVersioning<ID, NUMBER> {
    var versionId: ID
    var versionNumber: NUMBER?
}

class UuidEntityVersioning(
    override var versionNumber: Int? = null,
    versionId: UUID? = null
) : EntityVersioning<UUID, Int?> {
    override var versionId: UUID = versionId ?: UUID.randomUUID()
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
    EntityVersioning<UUID, Int?> by UuidEntityVersioning(),
    Published<UserT> by EntityPublishedImp(publishedBy)
