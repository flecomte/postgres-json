package fr.postgresjson.repository

import fr.postgresjson.connexion.Requester

interface RepositoryI {
    val requester: Requester

    enum class Direction {
        asc,
        desc
    }
}
