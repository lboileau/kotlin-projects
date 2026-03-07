package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.api.UpdatePlanParam
import com.acme.clients.planclient.internal.validations.ValidateUpdatePlan
import com.acme.clients.planclient.model.Plan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdatePlan(
    private val jdbi: Jdbi,
    private val getPlanById: GetPlanById
) {
    private val logger = LoggerFactory.getLogger(UpdatePlan::class.java)
    private val validate = ValidateUpdatePlan()

    fun execute(param: UpdatePlanParam): Result<Plan, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating plan id={}", param.id)
        return when (val existing = getPlanById.execute(GetByIdParam(param.id))) {
            is Result.Failure -> existing
            is Result.Success -> {
                val now = Instant.now()
                jdbi.withHandle<Unit, Exception> { handle ->
                    handle.createUpdate(
                        """
                        UPDATE plans SET name = :name, updated_at = :updatedAt
                        WHERE id = :id
                        """.trimIndent()
                    )
                        .bind("id", param.id)
                        .bind("name", param.name)
                        .bind("updatedAt", now)
                        .execute()
                }
                success(existing.value.copy(name = param.name, updatedAt = now))
            }
        }
    }
}
