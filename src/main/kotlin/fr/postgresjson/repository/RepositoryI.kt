package fr.postgresjson.repository

import fr.postgresjson.connexion.Requester

interface RepositoryI {
    var requester: Requester

    enum class Direction {
        asc,
        desc
    }
}
