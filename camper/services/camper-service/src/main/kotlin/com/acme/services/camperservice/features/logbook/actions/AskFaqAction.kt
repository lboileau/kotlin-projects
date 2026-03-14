package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.model.LogBookFaq
import com.acme.services.camperservice.features.logbook.params.AskFaqParam
import com.acme.services.camperservice.features.logbook.validations.ValidateAskFaq
import org.slf4j.LoggerFactory

internal class AskFaqAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(AskFaqAction::class.java)
    private val validate = ValidateAskFaq()

    fun execute(param: AskFaqParam): Result<LogBookFaq, LogBookError> {
        TODO("Not yet implemented")
    }
}
