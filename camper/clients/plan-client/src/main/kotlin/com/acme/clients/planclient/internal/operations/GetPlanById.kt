package com.acme.clients.planclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.planclient.api.GetByIdParam
import com.acme.clients.planclient.internal.adapters.PlanRowAdapter
import com.acme.clients.planclient.internal.validations.ValidateGetPlanById
import com.acme.clients.planclient.model.Plan
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetPlanById(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetPlanById::class.java)
    private val validate = ValidateGetPlanById()

    fun execute(param: GetByIdParam): Result<Plan, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Finding plan by id={}", param.id)
        val entity = jdbi.withHandle<Plan?, Exception> { handle ->
            handle.createQuery("SELECT id, name, visibility, owner_id, created_at, updated_at FROM plans WHERE id = :id")
                .bind("id", param.id)
                .map { rs, _ -> PlanRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("Plan", param.id.toString()))
    }
}
