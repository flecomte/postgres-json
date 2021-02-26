package fr.postgresjson.connexion

import fr.postgresjson.entity.EntityI
import kotlin.math.ceil

data class Paginated<T : EntityI>(
    val result: List<T>,
    val offset: Int,
    val limit: Int,
    val total: Int
) {
    val currentPage: Int = (offset / limit) + 1
    val count: Int = result.size
    val totalPages: Int = (total.toDouble() / limit.toDouble()).ceil()

    init {
        if (offset < 0) error("offset must be greather or equal than 0")
        if (limit < 1) error("limit must be greather or equal than 1")
        if (total < 0) error("total must be greather or equal than 0")
    }

    fun isLastPage(): Boolean = currentPage >= totalPages

    private fun Double.ceil(): Int = ceil(this).toInt()
}
