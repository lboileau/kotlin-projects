package com.acme.clients.mealplanclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.GetAccessParam
import com.acme.clients.mealplanclient.model.MealPlanAccess
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Resolves who owns a meal plan and whether a given caller is a member, in a single statement.
 * "Member" means the caller has access via the backing trip plan: either its `owner_id` or a row
 * in its `plan_members`. A meal plan with no backing plan (`plan_id IS NULL`) has no members.
 * Used by the service-layer authorizer to decide OWNER / MEMBER / no access.
 */
internal class GetAccess(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetAccess::class.java)

    fun execute(param: GetAccessParam): Result<MealPlanAccess, AppError> {
        logger.debug("Resolving access for mealPlanId={} userId={}", param.mealPlanId, param.userId)
        val access = jdbi.withHandle<MealPlanAccess?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT created_by,
                    EXISTS(
                        SELECT 1 FROM plans p WHERE p.id = meal_plans.plan_id AND p.owner_id = :userId
                        UNION
                        SELECT 1 FROM plan_members pm WHERE pm.plan_id = meal_plans.plan_id AND pm.user_id = :userId
                    ) AS is_member
                FROM meal_plans WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.mealPlanId)
                .bind("userId", param.userId)
                .map { rs, _ ->
                    MealPlanAccess(
                        createdBy = rs.getObject("created_by", UUID::class.java),
                        isMember = rs.getBoolean("is_member"),
                    )
                }
                .findOne()
                .orElse(null)
        }
        return if (access != null) success(access) else failure(NotFoundError("MealPlan", param.mealPlanId.toString()))
    }
}
