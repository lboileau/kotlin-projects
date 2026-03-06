package com.acme.services.camperservice.features.plan.actions

import com.acme.clients.common.Result
import com.acme.clients.planclient.api.GetByUserIdParam
import com.acme.clients.planclient.api.GetPublicPlansParam
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.plan.mapper.PlanMapper
import com.acme.services.camperservice.features.plan.model.Plan
import com.acme.services.camperservice.features.plan.params.GetPlansParam
import com.acme.services.camperservice.features.plan.validations.ValidateGetPlans
import org.slf4j.LoggerFactory

internal class GetPlansAction(private val planClient: PlanClient) {
    private val logger = LoggerFactory.getLogger(GetPlansAction::class.java)
    private val validate = ValidateGetPlans()

    fun execute(param: GetPlansParam): Result<List<Plan>, PlanError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Getting plans for user={}", param.userId)

        // Get user's plans (private + public they belong to)
        val userPlans = when (val result = planClient.getByUserId(GetByUserIdParam(param.userId))) {
            is Result.Success -> result.value.map { PlanMapper.fromClient(it) }
            is Result.Failure -> return Result.Failure(PlanError.fromClientError(result.error))
        }

        // Get all public plans
        val publicPlans = when (val result = planClient.getPublicPlans(GetPublicPlansParam())) {
            is Result.Success -> result.value.map { PlanMapper.fromClient(it) }
            is Result.Failure -> return Result.Failure(PlanError.fromClientError(result.error))
        }

        // Merge: user plans + public plans they're not already part of
        val userPlanIds = userPlans.map { it.id }.toSet()
        val merged = userPlans + publicPlans.filter { it.id !in userPlanIds }
        return Result.Success(merged)
    }
}
