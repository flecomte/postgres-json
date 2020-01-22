package fr.postgresjson.entity.immutable

import fr.postgresjson.entity.EntityI
import fr.postgresjson.entity.mutable.EntityDeletedAt
import fr.postgresjson.entity.mutable.EntityDeletedAtImp
import fr.postgresjson.entity.mutable.EntityDeletedBy
import fr.postgresjson.entity.mutable.EntityDeletedByImp
import org.joda.time.DateTime
import java.util.*

interface EntityRefI<T> : EntityI {
    val id: T
}

interface UuidEntityI : EntityRefI<UUID> {
    override val id: UUID
}

abstract class Entity<T>(override val id: T) : EntityRefI<T>
open class UuidEntity(override val id: UUID = UUID.randomUUID()) : UuidEntityI, Entity<UUID>(id)

/* Version */
interface EntityVersioning<ID, NUMBER> {
    val versionId: ID
    val versionNumber: NUMBER
}

class UuidEntityVersioning(
    override val versionNumber: Int,
    override val versionId: UUID = UUID.randomUUID()
) : EntityVersioning<UUID, Int>

/* Dates */
interface EntityCreatedAt {
    val createdAt: DateTime
}
interface EntityUpdatedAt {
    var updatedAt: DateTime
}

class EntityCreatedAtImp(
    override val createdAt: DateTime = DateTime.now()
) : EntityCreatedAt

class EntityUpdatedAtImp(
    override var updatedAt: DateTime = DateTime.now()
) : EntityUpdatedAt

/* Author */
interface EntityCreatedBy<T : EntityI> {
    val createdBy: T
}
interface EntityUpdatedBy<T : EntityI> {
    var updatedBy: T
}

class EntityCreatedByImp<UserT : EntityI>(
    override val createdBy: UserT
) : EntityCreatedBy<UserT>

class EntityUpdatedByImp<UserT : EntityI>(
    override var updatedBy: UserT
) : EntityUpdatedBy<UserT>

/* Mixed */
class EntityCreatedImp<UserT : EntityI>(
    override val createdAt: DateTime = DateTime.now(),
    createdBy: UserT
) : EntityCreatedBy<UserT> by EntityCreatedByImp(createdBy),
    EntityCreatedAt by EntityCreatedAtImp()

class EntityUpdatedImp<UserT : EntityI>(
    updatedAt: DateTime = DateTime.now(),
    override var updatedBy: UserT
) : EntityUpdatedBy<UserT>,
    EntityUpdatedAt by EntityUpdatedAtImp(updatedAt)

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