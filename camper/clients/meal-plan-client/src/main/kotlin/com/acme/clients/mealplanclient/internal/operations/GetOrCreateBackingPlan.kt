package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetShareTokenParam
import com.acme.clients.mealplanclient.internal.BackingPlanMarker
import com.acme.clients.mealplanclient.model.BackingPlanResolution
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Lazily creates a meal plan's backing trip plan on first call and returns its id — the id
 * doubles as the meal plan's share token, since there's no separate token column. The new plan's
 * `name` is set to [BackingPlanMarker.nameFor], not the meal plan's own name, so it can later be
 * told apart from a real trip the meal plan might otherwise be bound to (see [BackingPlanMarker]).
 *
 * If the meal plan already has a `plan_id` — because it's bound to an existing real trip, not
 * because it was already shared — that plan is returned as-is (unmarked); the caller
 * ([BackingPlanResolution.isRealTrip]) decides whether to reject issuing a share link for it.
 *
 * Race-safe via a `FOR UPDATE` lock on the meal plan row for the duration of the check-and-create
 * (same pattern as `AddRecipeToPlanIfAbsent`): two concurrent first calls serialise on the lock,
 * so only one of them creates the plan; the second sees `plan_id` already set and just returns
 * it. The new `plans` row and its owner's `plan_members` row are created the same way
 * `CreatePlanAction` creates a trip plan (owner inserted into `plan_members` too, so
 * `GetPlansByUserId`-style joins on `plan_members` also find it).
 */
internal class GetOrCreateBackingPlan(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetOrCreateBackingPlan::class.java)

    fun execute(param: GetShareTokenParam): Result<BackingPlanResolution, AppError> {
        logger.debug("Getting or creating backing plan for mealPlanId={}", param.mealPlanId)
        val resolution = jdbi.inTransaction<BackingPlanResolution?, Exception> { handle -> getOrCreate(handle, param.mealPlanId) }
        return if (resolution != null) success(resolution) else failure(NotFoundError("MealPlan", param.mealPlanId.toString()))
    }

    companion object {
        private fun getOrCreate(handle: Handle, mealPlanId: UUID): BackingPlanResolution? {
            val mealPlan = handle.createQuery("SELECT plan_id, created_by FROM meal_plans WHERE id = :id FOR UPDATE")
                .bind("id", mealPlanId)
                .map { rs, _ ->
                    rs.getObject("plan_id", UUID::class.java) to rs.getObject("created_by", UUID::class.java)
                }
                .findOne()
                .orElse(null) ?: return null

            val (existingPlanId, createdBy) = mealPlan
            if (existingPlanId != null) {
                val planName = handle.createQuery("SELECT name FROM plans WHERE id = :id")
                    .bind("id", existingPlanId)
                    .mapTo(String::class.java)
                    .findOne()
                    .orElse(null)
                return BackingPlanResolution(existingPlanId, isRealTrip = !BackingPlanMarker.isShareBacking(planName, mealPlanId))
            }

            val newPlanId = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO plans (id, name, visibility, owner_id, created_at, updated_at)
                VALUES (:id, :name, 'private', :ownerId, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", newPlanId)
                .bind("name", BackingPlanMarker.nameFor(mealPlanId))
                .bind("ownerId", createdBy)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()

            handle.createUpdate("INSERT INTO plan_members (plan_id, user_id, role, created_at) VALUES (:planId, :userId, 'member', :createdAt)")
                .bind("planId", newPlanId)
                .bind("userId", createdBy)
                .bind("createdAt", now)
                .execute()

            handle.createUpdate("UPDATE meal_plans SET plan_id = :planId WHERE id = :id")
                .bind("planId", newPlanId)
                .bind("id", mealPlanId)
                .execute()

            return BackingPlanResolution(newPlanId, isRealTrip = false)
        }
    }
}
