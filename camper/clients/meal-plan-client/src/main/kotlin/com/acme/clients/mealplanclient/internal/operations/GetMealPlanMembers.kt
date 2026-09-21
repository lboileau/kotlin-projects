package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetMealPlanMembersParam
import com.acme.clients.mealplanclient.model.MealPlanMember
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Retrieve everyone with access to a meal plan through its backing trip plan, *not counting the
 * meal plan's own owner*: the trip's `owner_id` first (if different from the meal plan's owner),
 * then its `plan_members` by join time. De-duplicated by user id. A meal plan with no backing
 * plan (`plan_id IS NULL`) has no members. Mirrors `member_count` in `MealPlanRowAdapter`.
 */
internal class GetMealPlanMembers(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetMealPlanMembers::class.java)

    fun execute(param: GetMealPlanMembersParam): Result<List<MealPlanMember>, AppError> {
        logger.debug("Fetching members for mealPlanId={}", param.mealPlanId)
        val members = jdbi.withHandle<List<MealPlanMember>, Exception> { handle ->
            handle.createQuery(
                """
                WITH mp AS (
                    SELECT id, plan_id, created_by FROM meal_plans WHERE id = :mealPlanId
                ),
                combined AS (
                    SELECT p.owner_id AS user_id, p.created_at AS joined_at, 0 AS ord
                    FROM mp JOIN plans p ON p.id = mp.plan_id
                    WHERE p.owner_id <> mp.created_by
                    UNION ALL
                    SELECT pm.user_id AS user_id, pm.created_at AS joined_at, 1 AS ord
                    FROM mp JOIN plan_members pm ON pm.plan_id = mp.plan_id
                    WHERE pm.user_id <> mp.created_by
                ),
                deduped AS (
                    SELECT DISTINCT ON (user_id) user_id, joined_at, ord
                    FROM combined
                    ORDER BY user_id, ord, joined_at
                )
                SELECT user_id, joined_at FROM deduped ORDER BY ord, joined_at
                """.trimIndent()
            )
                .bind("mealPlanId", param.mealPlanId)
                .map { rs, _ ->
                    MealPlanMember(
                        userId = rs.getObject("user_id", UUID::class.java),
                        createdAt = rs.getTimestamp("joined_at").toInstant(),
                    )
                }
                .list()
        }
        return success(members)
    }
}
