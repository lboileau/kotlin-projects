package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.DeleteFaqParam as ClientDeleteFaqParam
import com.acme.clients.logbookclient.api.GetFaqsByPlanIdParam as ClientGetFaqsByPlanIdParam
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.params.DeleteFaqParam
import com.acme.services.camperservice.features.logbook.validations.ValidateDeleteFaq
import org.slf4j.LoggerFactory

internal class DeleteFaqAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(DeleteFaqAction::class.java)
    private val validate = ValidateDeleteFaq()

    fun execute(param: DeleteFaqParam): Result<Unit, LogBookError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Fetch FAQs for the plan to find the one being deleted and check ownership
        val faqs = when (val result = logBookClient.getFaqsByPlanId(ClientGetFaqsByPlanIdParam(param.planId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(LogBookError.fromClientError(result.error))
        }
        val faq = faqs.find { it.id == param.faqId }
            ?: return Result.Failure(LogBookError.NotFound(param.faqId))

        // Authorize: OWNER, MANAGER, or the original asker
        if (faq.askedById != param.requestingUserId) {
            val authResult = planRoleAuthorizer.authorize(
                param.planId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER)
            )
            if (authResult is Result.Failure) {
                return Result.Failure(LogBookError.Forbidden(param.planId, param.requestingUserId))
            }
        }

        logger.debug("Deleting FAQ id={}", param.faqId)
        return when (val result = logBookClient.deleteFaq(ClientDeleteFaqParam(param.faqId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(LogBookError.fromClientError(result.error))
        }
    }
}
