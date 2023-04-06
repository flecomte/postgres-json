package fr.postgresjson.connexion

class DataNotFoundException(val queryExecuted: String): Exception() {
    override val message: String
        get() = "No data return for the query"
}