package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.params.RemoveActivityParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateRemoveActivity
import org.slf4j.LoggerFactory

internal class RemoveActivityAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(RemoveActivityAction::class.java)
    private val validate = ValidateRemoveActivity()

    fun execute(param: RemoveActivityParam): Result<Unit, LadderError> {
        TODO("Implementation in PR 5c — service implementation")
    }
}
