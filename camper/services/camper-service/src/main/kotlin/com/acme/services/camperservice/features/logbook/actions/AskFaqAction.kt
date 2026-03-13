package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.CreateFaqParam as ClientCreateFaqParam
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.mapper.LogBookMapper
import com.acme.services.camperservice.features.logbook.model.LogBookFaq
import com.acme.services.camperservice.features.logbook.params.AskFaqParam
import com.acme.services.camperservice.features.logbook.validations.ValidateAskFaq
import org.slf4j.LoggerFactory

internal class AskFaqAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(AskFaqAction::class.java)
    private val validate = ValidateAskFaq()

    fun execute(param: AskFaqParam): Result<LogBookFaq, LogBookError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val authResult = planRoleAuthorizer.authorize(
            param.planId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER, PlanRole.MEMBER)
        )
        if (authResult is Result.Failure) {
            return Result.Failure(LogBookError.Forbidden(param.planId, param.requestingUserId))
        }

        logger.debug("Asking FAQ in plan={}", param.planId)
        return when (val result = logBookClient.createFaq(
            ClientCreateFaqParam(
                planId = param.planId,
                question = param.question,
                askedById = param.requestingUserId,
            )
        )) {
            is Result.Success -> Result.Success(LogBookMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(LogBookError.fromClientError(result.error))
        }
    }
}
