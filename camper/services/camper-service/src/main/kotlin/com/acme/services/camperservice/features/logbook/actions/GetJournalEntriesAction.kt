package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
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
        TODO("Not yet implemented")
    }
}
