package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.RemoveMealPlanMemberParam
import com.acme.clients.mealplanclient.model.MealPlanMemberRemoval
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Remove a member from the backing trip plan's `plan_members`. If the target is the backing
 * plan's `owner_id`, nothing is deleted (there's no row to delete, and they can't be removed
 * this way) and [MealPlanMemberRemoval.IS_BACKING_PLAN_OWNER] is returned so the service layer
 * can surface a `400` instead of a silent no-op `204`.
 */
internal class RemoveMealPlanMember(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemoveMealPlanMember::class.java)

    fun execute(param: RemoveMealPlanMemberParam): Result<MealPlanMemberRemoval, AppError> {
        logger.debug("Removing member mealPlanId={} userId={}", param.mealPlanId, param.userId)
        val outcome = jdbi.withHandle<MealPlanMemberRemoval, Exception> { handle ->
            val backingPlanOwnerId = handle.createQuery(
                """
                SELECT p.owner_id
                FROM meal_plans mp
                JOIN plans p ON p.id = mp.plan_id
                WHERE mp.id = :mealPlanId
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .mapTo(UUID::class.java)
                .findOne()
                .orElse(null)

            if (backingPlanOwnerId != null && backingPlanOwnerId == param.userId) {
                MealPlanMemberRemoval.IS_BACKING_PLAN_OWNER
            } else {
                val removed = handle.createUpdate(
                    """
                    DELETE FROM plan_members
                    WHERE user_id = :userId
                      AND plan_id = (SELECT plan_id FROM meal_plans WHERE id = :mealPlanId)
                    """.trimIndent()
                )
                    .bind("mealPlanId", param.mealPlanId)
                    .bind("userId", param.userId)
                    .execute()
                if (removed > 0) MealPlanMemberRemoval.REMOVED else MealPlanMemberRemoval.NOT_A_MEMBER
            }
        }
        return success(outcome)
    }
}
