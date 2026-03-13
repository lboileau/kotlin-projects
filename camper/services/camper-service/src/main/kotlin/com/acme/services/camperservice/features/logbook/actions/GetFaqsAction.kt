package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.GetFaqsByPlanIdParam as ClientGetFaqsByPlanIdParam
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.mapper.LogBookMapper
import com.acme.services.camperservice.features.logbook.model.LogBookFaq
import com.acme.services.camperservice.features.logbook.params.GetFaqsParam
import com.acme.services.camperservice.features.logbook.validations.ValidateGetFaqs
import org.slf4j.LoggerFactory

internal class GetFaqsAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(GetFaqsAction::class.java)
    private val validate = ValidateGetFaqs()

    fun execute(param: GetFaqsParam): Result<List<LogBookFaq>, LogBookError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val authResult = planRoleAuthorizer.authorize(
            param.planId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER, PlanRole.MEMBER)
        )
        if (authResult is Result.Failure) {
            return Result.Failure(LogBookError.Forbidden(param.planId, param.requestingUserId))
        }

        logger.debug("Getting FAQs for plan={}", param.planId)
        return when (val result = logBookClient.getFaqsByPlanId(ClientGetFaqsByPlanIdParam(param.planId))) {
            is Result.Success -> Result.Success(result.value.map { LogBookMapper.fromClient(it) })
            is Result.Failure -> Result.Failure(LogBookError.fromClientError(result.error))
        }
    }
}
