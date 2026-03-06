package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.planclient.api.DeletePlanParam
import com.acme.clients.planclient.internal.validations.ValidateDeletePlan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class DeletePlan(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(DeletePlan::class.java)
    private val validate = ValidateDeletePlan()

    fun execute(param: DeletePlanParam): Result<Unit, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Deleting plan id={}", param.id)
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate("DELETE FROM plans WHERE id = :id")
                .bind("id", param.id)
                .execute()
        }
        return if (rowsAffected > 0) success(Unit) else failure(NotFoundError("Plan", param.id.toString()))
    }
}
