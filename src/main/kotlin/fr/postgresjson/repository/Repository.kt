package fr.postgresjson.repository

import fr.postgresjson.connexion.Requester
import fr.postgresjson.entity.EntityI
import kotlin.reflect.KClass

interface RepositoryI<E: EntityI<*>> {
    val entityName: KClass<E>
    var requester: Requester
    fun getClassName(): String {
        return entityName.simpleName!!
    }
}
