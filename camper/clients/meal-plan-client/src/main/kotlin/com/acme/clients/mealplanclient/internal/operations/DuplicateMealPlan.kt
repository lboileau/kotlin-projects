package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.DuplicateMealPlanParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRowAdapter
import com.acme.clients.mealplanclient.model.MealPlan
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Duplicates a meal plan (name, servings, scaling mode, days, and recipes; not purchases or
 * manual items) inside a single transaction, so the copy is either created in full or not at all.
 */
internal class DuplicateMealPlan(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DuplicateMealPlan::class.java)

    fun execute(param: DuplicateMealPlanParam): Result<MealPlan, AppError> {
        logger.debug("Duplicating meal plan sourceId={} name={}", param.sourceMealPlanId, param.name)
        val duplicated = jdbi.inTransaction<MealPlan?, Exception> { handle -> duplicate(handle, param) }
        return if (duplicated != null) success(duplicated) else failure(NotFoundError("MealPlan", param.sourceMealPlanId.toString()))
    }

    companion object {
        private fun duplicate(handle: Handle, param: DuplicateMealPlanParam): MealPlan? {
            val source = handle.createQuery(
                """
                SELECT id, plan_id, name, servings, scaling_mode, is_template, source_template_id, created_by, created_at, updated_at,
                    (SELECT COUNT(DISTINCT mpr.recipe_id) FROM meal_plan_recipes mpr JOIN meal_plan_days d ON d.id = mpr.meal_plan_day_id WHERE d.meal_plan_id = meal_plans.id) AS recipe_count
                FROM meal_plans WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.sourceMealPlanId)
                .map { rs, _ -> MealPlanRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null) ?: return null

            val newId = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO meal_plans (id, plan_id, name, servings, scaling_mode, is_template, source_template_id, created_by, created_at, updated_at)
                VALUES (:id, NULL, :name, :servings, :scalingMode, false, NULL, :createdBy, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", newId)
                .bind("name", param.name)
                .bind("servings", source.servings)
                .bind("scalingMode", source.scalingMode)
                .bind("createdBy", param.createdBy)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()

            // Copy days, preserving day numbers, tracking old day id -> new day id.
            val sourceDays = handle.createQuery("SELECT id, day_number FROM meal_plan_days WHERE meal_plan_id = :mealPlanId")
                .bind("mealPlanId", source.id)
                .map { rs, _ -> rs.getObject("id", UUID::class.java) to rs.getInt("day_number") }
                .list()

            val dayIdMap = mutableMapOf<UUID, UUID>()
            for ((oldDayId, dayNumber) in sourceDays) {
                val newDayId = UUID.randomUUID()
                handle.createUpdate(
                    """
                    INSERT INTO meal_plan_days (id, meal_plan_id, day_number, created_at, updated_at)
                    VALUES (:id, :mealPlanId, :dayNumber, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", newDayId)
                    .bind("mealPlanId", newId)
                    .bind("dayNumber", dayNumber)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                dayIdMap[oldDayId] = newDayId
            }

            // Copy recipes for the copied days, preserving meal type.
            if (dayIdMap.isNotEmpty()) {
                val sourceRecipes = handle.createQuery(
                    "SELECT meal_plan_day_id, meal_type, recipe_id FROM meal_plan_recipes WHERE meal_plan_day_id IN (<dayIds>)"
                )
                    .bindList("dayIds", dayIdMap.keys.toList())
                    .map { rs, _ ->
                        Triple(
                            rs.getObject("meal_plan_day_id", UUID::class.java),
                            rs.getString("meal_type"),
                            rs.getObject("recipe_id", UUID::class.java),
                        )
                    }
                    .list()

                for ((oldDayId, mealType, recipeId) in sourceRecipes) {
                    val newDayId = dayIdMap.getValue(oldDayId)
                    handle.createUpdate(
                        """
                        INSERT INTO meal_plan_recipes (id, meal_plan_day_id, meal_type, recipe_id, created_at, updated_at)
                        VALUES (:id, :dayId, :mealType, :recipeId, :createdAt, :updatedAt)
                        """.trimIndent()
                    )
                        .bind("id", UUID.randomUUID())
                        .bind("dayId", newDayId)
                        .bind("mealType", mealType)
                        .bind("recipeId", recipeId)
                        .bind("createdAt", now)
                        .bind("updatedAt", now)
                        .execute()
                }
            }

            return MealPlan(
                id = newId,
                planId = null,
                name = param.name,
                servings = source.servings,
                scalingMode = source.scalingMode,
                isTemplate = false,
                sourceTemplateId = null,
                createdBy = param.createdBy,
                createdAt = now,
                updatedAt = now,
                recipeCount = source.recipeCount,
            )
        }
    }
}
