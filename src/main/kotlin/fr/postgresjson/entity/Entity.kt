package fr.postgresjson.entity

import java.util.*

interface EntityI<T> {
    var id: T
}
abstract class Entity<T>(override var id: T) : EntityI<T>
abstract class UuidEntity(override var id: UUID = UUID.randomUUID()) : Entity<UUID>(id)
abstract class IdEntity(override var id: Int) : Entity<Int>(id)

interface EntityVersioning<T> {
    var version: T
}

interface EntityVersioningIncrement : EntityVersioning<Int>
