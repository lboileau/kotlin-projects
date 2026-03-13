package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
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
        TODO("Not yet implemented")
    }
}
