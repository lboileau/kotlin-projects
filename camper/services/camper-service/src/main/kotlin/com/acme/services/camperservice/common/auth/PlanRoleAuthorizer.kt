package com.acme.services.camperservice.common.auth

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.api.GetMembersParam
import com.acme.clients.planclient.api.PlanClient
import java.util.UUID

/**
 * Resolves a user's effective role in a plan and checks it against required roles.
 *
 * Role resolution:
 * 1. If userId == plan.ownerId → OWNER
 * 2. Else look up plan_members row → role "manager" maps to MANAGER, "member" maps to MEMBER
 * 3. If not found in plan_members and not owner → null (not a member)
 */
class PlanRoleAuthorizer(private val planClient: PlanClient) {

    /**
     * Authorize a user for a plan resource.
     * Returns Success(PlanRoleContext) if the user has one of the required roles,
     * or Failure(PlanRoleAuthorizationError) if not.
     */
    fun authorize(
        planId: UUID,
        userId: UUID,
        requiredRoles: Set<PlanRole>
    ): Result<PlanRoleContext, PlanRoleAuthorizationError> {
        // Fetch the plan to check ownership
        val plan = when (val planResult = planClient.getById(GetByIdParam(planId))) {
            is Result.Success -> planResult.value
            is Result.Failure -> return Result.Failure(
                PlanRoleAuthorizationError(planId, userId, requiredRoles, actualRole = null)
            )
        }

        // Resolve effective role
        val effectiveRole = if (plan.ownerId == userId) {
            PlanRole.OWNER
        } else {
            // Look up in plan_members
            val members = when (val membersResult = planClient.getMembers(GetMembersParam(planId))) {
                is Result.Success -> membersResult.value
                is Result.Failure -> return Result.Failure(
                    PlanRoleAuthorizationError(planId, userId, requiredRoles, actualRole = null)
                )
            }
            val member = members.find { it.userId == userId }
            when (member?.role) {
                "manager" -> PlanRole.MANAGER
                "member" -> PlanRole.MEMBER
                else -> null
            }
        }

        // Check against required roles
        return if (effectiveRole != null && effectiveRole in requiredRoles) {
            Result.Success(PlanRoleContext(planId, userId, effectiveRole))
        } else {
            Result.Failure(PlanRoleAuthorizationError(planId, userId, requiredRoles, effectiveRole))
        }
    }
}
