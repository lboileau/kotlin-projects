package com.acme.clients.itemclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itemclient.api.GetByPlanIdAndUserIdParam
import com.acme.clients.itemclient.internal.adapters.ItemRowAdapter
import com.acme.clients.itemclient.internal.validations.ValidateGetItemsByPlanIdAndUserId
import com.acme.clients.itemclient.model.Item
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetItemsByPlanIdAndUserId(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetItemsByPlanIdAndUserId::class.java)
    private val validate = ValidateGetItemsByPlanIdAndUserId()

    fun execute(param: GetByPlanIdAndUserIdParam): Result<List<Item>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding items for plan id={} user id={}", param.planId, param.userId)
        val entities = jdbi.withHandle<List<Item>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT i.id, i.plan_id, i.user_id, i.name, i.category, i.quantity, i.packed, i.gear_pack_id, gp.name AS gear_pack_name, i.created_at, i.updated_at
                FROM items i
                LEFT JOIN gear_packs gp ON gp.id = i.gear_pack_id
                WHERE i.plan_id = :planId AND i.user_id = :userId
                ORDER BY i.created_at
                """.trimIndent()
            )
                .bind("planId", param.planId)
                .bind("userId", param.userId)
                .map { rs, _ -> ItemRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
