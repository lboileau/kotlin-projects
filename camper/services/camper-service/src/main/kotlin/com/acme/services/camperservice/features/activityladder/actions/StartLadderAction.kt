package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.model.LadderDetail
import com.acme.services.camperservice.features.activityladder.params.StartLadderParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateStartLadder
import com.acme.services.camperservice.websocket.LadderEventPublisher
import org.slf4j.LoggerFactory

internal class StartLadderAction(
    private val ladderClient: ActivityLadderClient,
    private val eventPublisher: LadderEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(StartLadderAction::class.java)
    private val validate = ValidateStartLadder()

    fun execute(param: StartLadderParam): Result<LadderDetail, LadderError> {
        TODO("Implementation in PR 5c — service implementation")
    }
}
