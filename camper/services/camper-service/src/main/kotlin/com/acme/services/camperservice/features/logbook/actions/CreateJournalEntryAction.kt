package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
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
        TODO("Not yet implemented")
    }
}
