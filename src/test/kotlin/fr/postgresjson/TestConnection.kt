package fr.postgresjson

import fr.postgresjson.connexion.Connection

fun TestConnection(): Connection =
    Connection(database = "json_test", username = "test", password = "test", port = 35555)

fun <A> Connection.rollbackAfter(block: Connection.() -> A?) = connect().run {
    sendQuery("BEGIN")
    try {
        block().apply { sendQuery("ROLLBACK") }
    } catch (e: Throwable) {
        sendQuery("ROLLBACK")
        throw e
    }
}
