package com.acme.clients.itemclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.GetByUserIdParam
import com.acme.clients.itemclient.internal.adapters.ItemRowAdapter
import com.acme.clients.itemclient.internal.validations.ValidateGetItemsByUserId
import com.acme.clients.itemclient.model.Item
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetItemsByUserId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetItemsByUserId::class.java)
    private val validate = ValidateGetItemsByUserId()

    fun execute(param: GetByUserIdParam): Result<List<Item>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding items for user id={}", param.userId)
        val entities = jdbi.withHandle<List<Item>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, plan_id, user_id, name, category, quantity, packed, created_at, updated_at
                FROM items
                WHERE user_id = :userId
                ORDER BY created_at
                """.trimIndent()
            )
                .bind("userId", param.userId)
                .map { rs, _ -> ItemRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
