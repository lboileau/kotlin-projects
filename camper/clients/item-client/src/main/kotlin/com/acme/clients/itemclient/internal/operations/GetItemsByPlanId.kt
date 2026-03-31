package com.acme.clients.itemclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.GetByPlanIdParam
import com.acme.clients.itemclient.internal.adapters.ItemRowAdapter
import com.acme.clients.itemclient.internal.validations.ValidateGetItemsByPlanId
import com.acme.clients.itemclient.model.Item
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetItemsByPlanId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetItemsByPlanId::class.java)
    private val validate = ValidateGetItemsByPlanId()

    fun execute(param: GetByPlanIdParam): Result<List<Item>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding items for plan id={}", param.planId)
        val entities = jdbi.withHandle<List<Item>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, plan_id, user_id, name, category, quantity, packed, created_at, updated_at
                FROM items
                WHERE plan_id = :planId
                AND user_id IS NULL
                ORDER BY created_at
                """.trimIndent()
            )
                .bind("planId", param.planId)
                .map { rs, _ -> ItemRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
