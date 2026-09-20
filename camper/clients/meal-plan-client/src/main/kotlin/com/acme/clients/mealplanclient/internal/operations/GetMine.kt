package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetMineParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRowAdapter
import com.acme.clients.mealplanclient.internal.adapters.MealPlanSelect
import com.acme.clients.mealplanclient.model.MealPlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

/**
 * Meal plans a user owns plus meal plans shared with them, newest updated first. "Shared with
 * them" means the caller has access via the backing trip plan: either its `owner_id` or a row in
 * its `plan_members`.
 */
internal class GetMine(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetMine::class.java)

    fun execute(param: GetMineParam): Result<List<MealPlan>, AppError> {
        logger.debug("Fetching meal plans owned by or shared with userId={}", param.userId)
        val entities = jdbi.withHandle<List<MealPlan>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT ${MealPlanSelect.COLUMNS}
                FROM meal_plans
                WHERE created_by = :userId
                   OR EXISTS (SELECT 1 FROM plans p WHERE p.id = meal_plans.plan_id AND p.owner_id = :userId)
                   OR EXISTS (SELECT 1 FROM plan_members pm WHERE pm.plan_id = meal_plans.plan_id AND pm.user_id = :userId)
                ORDER BY updated_at DESC
                """.trimIndent()
            )
                .bind("userId", param.userId)
                .map { rs, _ -> MealPlanRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
