package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.AddMealPlanMemberParam
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

/**
 * Idempotent insert into the backing trip plan's `plan_members` (role 'member'). If the user is
 * already the backing plan's `owner_id`, they already have implicit access, so no row is
 * inserted and false is returned. Returns whether a new row was actually created.
 */
internal class AddMealPlanMember(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddMealPlanMember::class.java)

    fun execute(param: AddMealPlanMemberParam): Result<Boolean, AppError> {
        logger.debug("Adding member mealPlanId={} userId={}", param.mealPlanId, param.userId)
        val inserted = jdbi.withHandle<Boolean, Exception> { handle ->
            val isBackingPlanOwner = handle.createQuery(
                """
                SELECT p.owner_id = :userId
                FROM meal_plans mp
                JOIN plans p ON p.id = mp.plan_id
                WHERE mp.id = :mealPlanId
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .bind("userId", param.userId)
                .mapTo(Boolean::class.java)
                .findOne()
                .orElse(false)

            if (isBackingPlanOwner) {
                false
            } else {
                val rows = handle.createUpdate(
                    """
                    INSERT INTO plan_members (plan_id, user_id, role)
                    SELECT plan_id, :userId, 'member' FROM meal_plans WHERE id = :mealPlanId
                    ON CONFLICT (plan_id, user_id) DO NOTHING
                    """.trimIndent()
                )
                    .bind("mealPlanId", param.mealPlanId)
                    .bind("userId", param.userId)
                    .execute()
                rows > 0
            }
        }
        return success(inserted)
    }
}
