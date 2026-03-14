package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.GetJournalEntriesByPlanIdParam as ClientGetJournalEntriesByPlanIdParam
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.mapper.LogBookMapper
import com.acme.services.camperservice.features.logbook.model.LogBookJournalEntry
import com.acme.services.camperservice.features.logbook.params.GetJournalEntriesParam
import com.acme.services.camperservice.features.logbook.validations.ValidateGetJournalEntries
import org.slf4j.LoggerFactory

internal class GetJournalEntriesAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(GetJournalEntriesAction::class.java)
    private val validate = ValidateGetJournalEntries()

    fun execute(param: GetJournalEntriesParam): Result<List<LogBookJournalEntry>, LogBookError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val authResult = planRoleAuthorizer.authorize(
            param.planId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER, PlanRole.MEMBER)
        )
        if (authResult is Result.Failure) {
            return Result.Failure(LogBookError.Forbidden(param.planId, param.requestingUserId))
        }

        logger.debug("Getting journal entries for plan={}", param.planId)
        return when (val result = logBookClient.getJournalEntriesByPlanId(ClientGetJournalEntriesByPlanIdParam(param.planId))) {
            is Result.Success -> Result.Success(result.value.map { LogBookMapper.fromClient(it) })
            is Result.Failure -> Result.Failure(LogBookError.fromClientError(result.error))
        }
    }
}
