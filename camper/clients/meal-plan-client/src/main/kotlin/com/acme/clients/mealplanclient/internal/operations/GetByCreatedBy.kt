package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetByCreatedByParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRowAdapter
import com.acme.clients.mealplanclient.model.MealPlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetByCreatedBy(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetByCreatedBy::class.java)

    fun execute(param: GetByCreatedByParam): Result<List<MealPlan>, AppError> {
        logger.debug("Fetching meal plans created by createdBy={}", param.createdBy)
        val entities = jdbi.withHandle<List<MealPlan>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, plan_id, name, servings, scaling_mode, is_template, source_template_id, created_by, created_at, updated_at
                FROM meal_plans WHERE created_by = :createdBy ORDER BY updated_at DESC
                """.trimIndent()
            )
                .bind("createdBy", param.createdBy)
                .map { rs, _ -> MealPlanRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
