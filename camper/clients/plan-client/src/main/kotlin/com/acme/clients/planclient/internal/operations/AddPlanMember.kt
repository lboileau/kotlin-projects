package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.planclient.api.AddMemberParam
import com.acme.clients.planclient.internal.validations.ValidateAddMember
import com.acme.clients.planclient.model.PlanMember
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class AddPlanMember(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddPlanMember::class.java)
    private val validate = ValidateAddMember()

    fun execute(param: AddMemberParam): Result<PlanMember, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding member userId={} to plan id={}", param.userId, param.planId)
        return try {
            val now = Instant.now()
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate(
                    """
                    INSERT INTO plan_members (plan_id, user_id, role, created_at)
                    VALUES (:planId, :userId, 'member', :createdAt)
                    """.trimIndent()
                )
                    .bind("planId", param.planId)
                    .bind("userId", param.userId)
                    .bind("createdAt", now)
                    .execute()
            }
            success(PlanMember(planId = param.planId, userId = param.userId, role = "member", createdAt = now))
        } catch (e: Exception) {
            if (e.message?.contains("duplicate key") == true || e.message?.contains("plan_members_pkey") == true) {
                failure(ConflictError("PlanMember", "user is already a member of this plan"))
            } else {
                throw e
            }
        }
    }
}
