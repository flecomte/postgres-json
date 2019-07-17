package fr.postgresjson.stopwatch

fun <T> elapse(callback: (start: Long) -> T, after: (elapse: Long) -> Unit): T {
    val start = System.currentTimeMillis()
    val result = callback(start)
    after(System.currentTimeMillis() - start)
    return result
}
