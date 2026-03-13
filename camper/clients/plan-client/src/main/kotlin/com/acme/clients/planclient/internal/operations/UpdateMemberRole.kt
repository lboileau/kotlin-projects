package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.planclient.api.UpdateMemberRoleParam
import com.acme.clients.planclient.internal.adapters.PlanMemberRowAdapter
import com.acme.clients.planclient.internal.validations.ValidateUpdateMemberRole
import com.acme.clients.planclient.model.PlanMember
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class UpdateMemberRole(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpdateMemberRole::class.java)
    private val validate = ValidateUpdateMemberRole()

    fun execute(param: UpdateMemberRoleParam): Result<PlanMember, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating role for userId={} in plan id={} to role={}", param.userId, param.planId, param.role)
        val member = jdbi.withHandle<PlanMember?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE plan_members SET role = :role
                WHERE plan_id = :planId AND user_id = :userId
                RETURNING plan_id, user_id, role, created_at
                """.trimIndent()
            )
                .bind("planId", param.planId)
                .bind("userId", param.userId)
                .bind("role", param.role)
                .map { rs, _ -> PlanMemberRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (member != null) {
            success(member)
        } else {
            failure(NotFoundError("PlanMember", "${param.planId}/${param.userId}"))
        }
    }
}
