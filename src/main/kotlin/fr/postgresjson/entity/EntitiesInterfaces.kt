package fr.postgresjson.entity

import org.joda.time.DateTime
import java.util.UUID

interface EntityRefI<T> : EntityI {
    val id: T
}

interface UuidEntityI : EntityRefI<UUID> {
    override val id: UUID
}

abstract class Entity<T>(override val id: T) : EntityRefI<T>
open class UuidEntity(id: UUID? = null) : UuidEntityI, Entity<UUID>(id ?: UUID.randomUUID())

/* Version */
interface EntityVersioning<ID, NUMBER> {
    val versionNumber: NUMBER
    val versionId: ID
}

class UuidEntityVersioning(
    override val versionNumber: Int,
    versionId: UUID? = null
) : EntityVersioning<UUID, Int> {
    override val versionId: UUID = versionId ?: UUID.randomUUID()
}

/* Dates */
interface EntityCreatedAt {
    val createdAt: DateTime
}
interface EntityUpdatedAt {
    val updatedAt: DateTime
}

interface EntityDeletedAt {
    val deletedAt: DateTime?
    fun isDeleted(): Boolean {
        return deletedAt?.let {
            it < DateTime.now()
        } ?: false
    }
}

class EntityCreatedAtImp(
    override val createdAt: DateTime = DateTime.now()
) : EntityCreatedAt

class EntityUpdatedAtImp(
    override val updatedAt: DateTime = DateTime.now()
) : EntityUpdatedAt

class EntityDeletedAtImp(
    override val deletedAt: DateTime? = null
) : EntityDeletedAt

/* Author */
interface EntityCreatedBy<T : EntityI> {
    val createdBy: T
}
interface EntityUpdatedBy<T : EntityI> {
    val updatedBy: T
}

interface EntityDeletedBy<T : EntityI> {
    val deletedBy: T?
}

class EntityCreatedByImp<UserT : EntityI>(
    override val createdBy: UserT
) : EntityCreatedBy<UserT>

class EntityUpdatedByImp<UserT : EntityI>(
    override val updatedBy: UserT
) : EntityUpdatedBy<UserT>

class EntityDeletedByImp<UserT : EntityI>(
    override val deletedBy: UserT?
) : EntityDeletedBy<UserT>

/* Mixed */
class EntityCreatedImp<UserT : EntityI>(
    override val createdAt: DateTime = DateTime.now(),
    createdBy: UserT
) : EntityCreatedBy<UserT> by EntityCreatedByImp(createdBy),
    EntityCreatedAt by EntityCreatedAtImp()

class EntityUpdatedImp<UserT : EntityI>(
    updatedAt: DateTime = DateTime.now(),
    override val updatedBy: UserT
) : EntityUpdatedBy<UserT>,
    EntityUpdatedAt by EntityUpdatedAtImp(updatedAt)

/* Published */
interface Published<UserT : EntityI> {
    val publishedAt: DateTime?
    val publishedBy: UserT?
}

class EntityPublishedImp<UserT : EntityI>(
    override val publishedBy: UserT?
) : Published<UserT> {
    override val publishedAt: DateTime? = null
}

/* Implementation */
abstract class EntityImp<T, UserT : EntityI>(
    updatedBy: UserT,
    updatedAt: DateTime = DateTime.now()
) : UuidEntity(),
    EntityCreatedAt by EntityCreatedAtImp(updatedAt),
    EntityUpdatedAt by EntityUpdatedAtImp(updatedAt),
    EntityDeletedAt by EntityDeletedAtImp(),
    EntityCreatedBy<UserT> by EntityCreatedByImp(updatedBy),
    EntityUpdatedBy<UserT> by EntityUpdatedByImp(updatedBy),
    EntityDeletedBy<UserT> by EntityDeletedByImp(updatedBy)

abstract class UuidEntityExtended<T, UserT : EntityI>(
    updatedBy: UserT,
    publishedBy: UserT?
) :
    EntityImp<T, UserT>(updatedBy),
    EntityVersioning<UUID, Int> by UuidEntityVersioning(0),
    Published<UserT> by EntityPublishedImp(publishedBy)
