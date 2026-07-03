package no.iktdev.kammich.database

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun <T> withTransaction(
    rollbackOnFailure: Boolean = false,
    run: () -> T
): Result<T> {
    return try {
        val result = transaction {
            try {
                run()
            } catch (e: Exception) {
                if (rollbackOnFailure) rollback()
                throw e
            }
        }
        Result.success(result)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}


fun Column<String>.likeAny(values: List<String>): Op<Boolean> =
    values
        .map { this like "%$it%" }
        .reduce(Op<Boolean>::or)
