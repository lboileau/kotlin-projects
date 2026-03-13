package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.GetJournalEntriesByPlanIdParam as ClientGetJournalEntriesByPlanIdParam
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.clients.logbookclient.api.UpdateJournalEntryParam as ClientUpdateJournalEntryParam
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.mapper.LogBookMapper
import com.acme.services.camperservice.features.logbook.model.LogBookJournalEntry
import com.acme.services.camperservice.features.logbook.params.UpdateJournalEntryParam
import com.acme.services.camperservice.features.logbook.validations.ValidateUpdateJournalEntry
import org.slf4j.LoggerFactory

internal class UpdateJournalEntryAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(UpdateJournalEntryAction::class.java)
    private val validate = ValidateUpdateJournalEntry()

    fun execute(param: UpdateJournalEntryParam): Result<LogBookJournalEntry, LogBookError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        // Authorize as any plan member first
        val authResult = planRoleAuthorizer.authorize(
            param.planId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER, PlanRole.MEMBER)
        )
        if (authResult is Result.Failure) {
            return Result.Failure(LogBookError.Forbidden(param.planId, param.requestingUserId))
        }

        // Fetch the entry to verify original author
        val entries = when (val result = logBookClient.getJournalEntriesByPlanId(ClientGetJournalEntriesByPlanIdParam(param.planId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(LogBookError.fromClientError(result.error))
        }
        val entry = entries.find { it.id == param.entryId }
            ?: return Result.Failure(LogBookError.NotFound(param.entryId))

        // Only the original author can update
        if (entry.userId != param.requestingUserId) {
            return Result.Failure(LogBookError.Forbidden(param.planId, param.requestingUserId))
        }

        logger.debug("Updating journal entry id={}", param.entryId)
        return when (val result = logBookClient.updateJournalEntry(
            ClientUpdateJournalEntryParam(
                id = param.entryId,
                content = param.content,
            )
        )) {
            is Result.Success -> Result.Success(LogBookMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(LogBookError.fromClientError(result.error))
        }
    }
}
