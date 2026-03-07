package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.planclient.api.RemoveMemberParam
import com.acme.clients.planclient.internal.validations.ValidateRemoveMember
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class RemovePlanMember(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(RemovePlanMember::class.java)
    private val validate = ValidateRemoveMember()

    fun execute(param: RemoveMemberParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Removing member userId={} from plan id={}", param.userId, param.planId)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM plan_members WHERE plan_id = :planId AND user_id = :userId")
                .bind("planId", param.planId)
                .bind("userId", param.userId)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("PlanMember", "${param.planId}/${param.userId}"))
    }
}
