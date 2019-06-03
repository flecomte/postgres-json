package fr.postgresjson.entity

import java.util.*

interface EntityI<T> {
    var id: T?
}
abstract class Entity<T>(override var id: T? = null) : EntityI<T?>
abstract class UuidEntity(override var id: UUID? = UUID.randomUUID()) : Entity<UUID?>(id)
abstract class IdEntity(override var id: Int? = null) : Entity<Int?>(id)

interface EntityVersioning<T> {
    var version: T
}

interface EntityVersioningIncrement : EntityVersioning<Int>
