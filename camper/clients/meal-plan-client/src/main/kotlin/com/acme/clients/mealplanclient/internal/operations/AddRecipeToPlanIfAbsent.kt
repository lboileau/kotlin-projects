package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.AddRecipeToPlanIfAbsentParam
import com.acme.clients.mealplanclient.internal.adapters.MealPlanRecipeRowAdapter
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Atomically finds-or-creates a meal_plan_recipes row for (mealPlanId, recipeId).
 *
 * meal_plan_recipes has no DB-level uniqueness per plan, so a plain read-then-insert would let two
 * concurrent callers both pass the "already present" check and double-insert the same recipe. This
 * takes a `FOR UPDATE` lock on the meal plan row for the duration of the check-and-insert (same
 * pattern as `withLadderLocked` in activity-ladder-client), so every concurrent caller for the same
 * plan serialises on that lock. Because of the lock, the (meal_plan_id, day_number) unique
 * constraint on a concurrently-created day 1 is unreachable from inside this operation.
 */
internal class AddRecipeToPlanIfAbsent(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddRecipeToPlanIfAbsent::class.java)

    /** Returns the meal_plan_recipes row plus whether it was newly created (false = already present). */
    fun execute(param: AddRecipeToPlanIfAbsentParam): Result<Pair<MealPlanRecipe, Boolean>, AppError> {
        logger.debug("Adding recipe recipeId={} to plan mealPlanId={} if absent", param.recipeId, param.mealPlanId)
        val outcome = jdbi.inTransaction<Outcome?, Exception> { handle -> addIfAbsent(handle, param) }
        return if (outcome != null) success(outcome.recipe to outcome.created) else failure(NotFoundError("MealPlan", param.mealPlanId.toString()))
    }

    private data class Outcome(val recipe: MealPlanRecipe, val created: Boolean)

    companion object {
        private fun addIfAbsent(handle: Handle, param: AddRecipeToPlanIfAbsentParam): Outcome? {
            val locked = handle.createQuery("SELECT id FROM meal_plans WHERE id = :id FOR UPDATE")
                .bind("id", param.mealPlanId)
                .mapTo(UUID::class.java)
                .findOne()
            if (locked.isEmpty) return null

            val existing = handle.createQuery(
                """
                SELECT mpr.id, mpr.meal_plan_day_id, mpr.meal_type, mpr.recipe_id, mpr.created_at, mpr.updated_at
                FROM meal_plan_recipes mpr
                JOIN meal_plan_days mpd ON mpd.id = mpr.meal_plan_day_id
                WHERE mpd.meal_plan_id = :mealPlanId AND mpr.recipe_id = :recipeId
                LIMIT 1
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .bind("recipeId", param.recipeId)
                .map { rs, _ -> MealPlanRecipeRowAdapter.fromResultSet(rs) }
                .findFirst()

            if (existing.isPresent) {
                return Outcome(existing.get(), created = false)
            }

            val now = Instant.now()
            val lowestDayId = handle.createQuery(
                "SELECT id FROM meal_plan_days WHERE meal_plan_id = :mealPlanId ORDER BY day_number ASC LIMIT 1"
            )
                .bind("mealPlanId", param.mealPlanId)
                .mapTo(UUID::class.java)
                .findOne()
                .orElseGet {
                    val newDayId = UUID.randomUUID()
                    handle.createUpdate(
                        """
                        INSERT INTO meal_plan_days (id, meal_plan_id, day_number, created_at, updated_at)
                        VALUES (:id, :mealPlanId, 1, :createdAt, :updatedAt)
                        """.trimIndent()
                    )
                        .bind("id", newDayId)
                        .bind("mealPlanId", param.mealPlanId)
                        .bind("createdAt", now)
                        .bind("updatedAt", now)
                        .execute()
                    newDayId
                }

            val newId = UUID.randomUUID()
            handle.createUpdate(
                """
                INSERT INTO meal_plan_recipes (id, meal_plan_day_id, meal_type, recipe_id, created_at, updated_at)
                VALUES (:id, :dayId, 'dinner', :recipeId, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", newId)
                .bind("dayId", lowestDayId)
                .bind("recipeId", param.recipeId)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()

            return Outcome(
                MealPlanRecipe(
                    id = newId,
                    mealPlanDayId = lowestDayId,
                    mealType = "dinner",
                    recipeId = param.recipeId,
                    createdAt = now,
                    updatedAt = now,
                ),
                created = true,
            )
        }
    }
}
