package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.LogBookClient
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
        TODO("Not yet implemented")
    }
}
