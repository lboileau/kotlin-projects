package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.CreateJournalEntryParam as ClientCreateJournalEntryParam
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRole
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.mapper.LogBookMapper
import com.acme.services.camperservice.features.logbook.model.LogBookJournalEntry
import com.acme.services.camperservice.features.logbook.params.CreateJournalEntryParam
import com.acme.services.camperservice.features.logbook.validations.ValidateCreateJournalEntry
import org.slf4j.LoggerFactory

internal class CreateJournalEntryAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(CreateJournalEntryAction::class.java)
    private val validate = ValidateCreateJournalEntry()

    fun execute(param: CreateJournalEntryParam): Result<LogBookJournalEntry, LogBookError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val authResult = planRoleAuthorizer.authorize(
            param.planId, param.requestingUserId, setOf(PlanRole.OWNER, PlanRole.MANAGER, PlanRole.MEMBER)
        )
        if (authResult is Result.Failure) {
            return Result.Failure(LogBookError.Forbidden(param.planId, param.requestingUserId))
        }

        logger.debug("Creating journal entry in plan={}", param.planId)
        return when (val result = logBookClient.createJournalEntry(
            ClientCreateJournalEntryParam(
                planId = param.planId,
                userId = param.requestingUserId,
                content = param.content,
            )
        )) {
            is Result.Success -> Result.Success(LogBookMapper.fromClient(result.value))
            is Result.Failure -> Result.Failure(LogBookError.fromClientError(result.error))
        }
    }
}
