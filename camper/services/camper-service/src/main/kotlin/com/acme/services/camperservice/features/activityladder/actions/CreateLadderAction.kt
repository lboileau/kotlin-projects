package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.model.LadderDetail
import com.acme.services.camperservice.features.activityladder.params.CreateLadderParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateCreateLadder
import org.slf4j.LoggerFactory

internal class CreateLadderAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(CreateLadderAction::class.java)
    private val validate = ValidateCreateLadder()

    fun execute(param: CreateLadderParam): Result<LadderDetail, LadderError> {
        TODO("Implementation in PR 5c — service implementation")
    }
}
