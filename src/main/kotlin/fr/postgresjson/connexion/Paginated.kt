package fr.postgresjson.connexion

import com.github.jasync.sql.db.util.length
import fr.postgresjson.entity.EntityI

data class Paginated<T: EntityI<*>>(
    val result: List<T>,
    val offset: Int,
    val limit: Int,
    val total: Int
) {
    val currentPage: Int = (offset / limit) + 1
    val count: Int = result.length

    init {
        if (offset < 0) error("offset must be greather or equal than 0")
        if (limit < 1) error("limit must be greather than 1")
        if (total < 1) error("total must be greather or equal than 0")
    }
}