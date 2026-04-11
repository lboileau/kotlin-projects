package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.model.LadderSummary
import com.acme.services.camperservice.features.activityladder.params.ListLaddersParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateListLadders
import org.slf4j.LoggerFactory

internal class ListLaddersAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(ListLaddersAction::class.java)
    private val validate = ValidateListLadders()

    fun execute(param: ListLaddersParam): Result<List<LadderSummary>, LadderError> {
        TODO("Implementation in PR 5c — service implementation")
    }
}
