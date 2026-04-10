package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.model.Ladder
import com.acme.services.camperservice.features.activityladder.params.RestartLadderParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateRestartLadder
import org.slf4j.LoggerFactory

internal class RestartLadderAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(RestartLadderAction::class.java)
    private val validate = ValidateRestartLadder()

    fun execute(param: RestartLadderParam): Result<Ladder, LadderError> {
        TODO("Implementation in PR 5c — service implementation")
    }
}
