package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetByPlanIdParam
import com.acme.clients.mealplanclient.internal.BackingPlanMarker
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRowAdapter
import com.acme.clients.mealplanclient.internal.adapters.MealPlanSelect
import com.acme.clients.mealplanclient.model.MealPlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

/**
 * Resolves a share token (a `plans.id`) to its meal plan, but ONLY when that plan was actually
 * created by the share flow — i.e. its name is [BackingPlanMarker.nameFor] the meal plan attached
 * to it. Returns null both when no meal plan has that `plan_id` at all, and when one does but the
 * plan is a real trip the meal plan happens to be bound to (not something a link should grant
 * access to) — the two cases are deliberately indistinguishable to the caller, so accept-invite
 * can't be used to self-join an arbitrary trip by guessing its id. See [BackingPlanMarker].
 */
internal class GetShareBackingMealPlan(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetShareBackingMealPlan::class.java)

    fun execute(param: GetByPlanIdParam): Result<MealPlan?, AppError> {
        logger.debug("Finding share-backing meal plan for planId={}", param.planId)
        val entity = jdbi.withHandle<MealPlan?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT ${MealPlanSelect.COLUMNS}
                FROM meal_plans
                WHERE meal_plans.plan_id = :planId
                  AND EXISTS (
                      SELECT 1 FROM plans
                      WHERE plans.id = meal_plans.plan_id
                        AND plans.name = '${BackingPlanMarker.PREFIX}' || meal_plans.id::text
                  )
                """.trimIndent()
            )
                .bind("planId", param.planId)
                .map { rs, _ -> MealPlanRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return success(entity)
    }
}
