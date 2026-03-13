package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.DeleteJournalEntryParam as ClientDeleteJournalEntryParam
import com.acme.clients.logbookclient.api.GetJournalEntriesByPlanIdParam as ClientGetJournalEntriesByPlanIdParam
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.params.DeleteJournalEntryParam
import com.acme.services.camperservice.features.logbook.validations.ValidateDeleteJournalEntry
import org.slf4j.LoggerFactory

internal class DeleteJournalEntryAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(DeleteJournalEntryAction::class.java)
    private val validate = ValidateDeleteJournalEntry()

    fun execute(param: DeleteJournalEntryParam): Result<Unit, LogBookError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Fetch entries for the plan to find the one being deleted and check authorship
        val entries = when (val result = logBookClient.getJournalEntriesByPlanId(ClientGetJournalEntriesByPlanIdParam(param.planId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(LogBookError.fromClientError(result.error))
        }
        val entry = entries.find { it.id == param.entryId }
            ?: return Result.Failure(LogBookError.NotFound(param.entryId))

        // Authorize: OWNER, MANAGER, or the original author
        if (entry.userId != param.requestingUserId) {
            val authResult = planRoleAuthorizer.authorize(
                param.planId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER)
            )
            if (authResult is Result.Failure) {
                return Result.Failure(LogBookError.Forbidden(param.planId, param.requestingUserId))
            }
        }

        logger.debug("Deleting journal entry id={}", param.entryId)
        return when (val result = logBookClient.deleteJournalEntry(ClientDeleteJournalEntryParam(param.entryId))) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(LogBookError.fromClientError(result.error))
        }
    }
}
