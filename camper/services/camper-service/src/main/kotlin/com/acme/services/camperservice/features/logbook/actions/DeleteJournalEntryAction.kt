package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.LogBookClient
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
        TODO("Not yet implemented")
    }
}
