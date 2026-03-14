package com.acme.services.camperservice.features.logbook.actions

import com.acme.clients.common.Result
import com.acme.clients.logbookclient.api.LogBookClient
import com.acme.services.camperservice.common.auth.PlanRoleAuthorizer
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.logbook.model.LogBookFaq
import com.acme.services.camperservice.features.logbook.params.GetFaqsParam
import com.acme.services.camperservice.features.logbook.validations.ValidateGetFaqs
import org.slf4j.LoggerFactory

internal class GetFaqsAction(
    private val logBookClient: LogBookClient,
    private val planRoleAuthorizer: PlanRoleAuthorizer,
) {
    private val logger = LoggerFactory.getLogger(GetFaqsAction::class.java)
    private val validate = ValidateGetFaqs()

    fun execute(param: GetFaqsParam): Result<List<LogBookFaq>, LogBookError> {
        TODO("Not yet implemented")
    }
}
