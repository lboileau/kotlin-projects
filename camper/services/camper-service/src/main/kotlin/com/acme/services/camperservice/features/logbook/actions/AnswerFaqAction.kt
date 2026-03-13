package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.AnswerFaqParam as ClientAnswerFaqParam
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.mapper.LogBookMapper
import com.acme.services.camperservice.features.logbook.model.LogBookFaq
import com.acme.services.camperservice.features.logbook.params.AnswerFaqParam
import com.acme.services.camperservice.features.logbook.validations.ValidateAnswerFaq
import org.slf4j.LoggerFactory

internal class AnswerFaqAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(AnswerFaqAction::class.java)
    private val validate = ValidateAnswerFaq()

    fun execute(param: AnswerFaqParam): Result<LogBookFaq, LogBookError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val authResult = planRoleAuthorizer.authorize(
            param.planId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER)
        )
        if (authResult is Result.Failure) {
            return Result.Failure(LogBookError.Forbidden(param.planId, param.requestingUserId))
        }

        logger.debug("Answering FAQ id={}", param.faqId)
        return when (val result = logBookClient.answerFaq(
            ClientAnswerFaqParam(
                id = param.faqId,
                answer = param.answer,
                answeredById = param.requestingUserId,
            )
        )) {
            is Result.Success -> Result.Success(LogBookMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(LogBookError.fromClientError(result.error))
        }
    }
}
