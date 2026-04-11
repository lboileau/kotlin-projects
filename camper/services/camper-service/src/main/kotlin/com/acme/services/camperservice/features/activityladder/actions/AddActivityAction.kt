package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.model.LadderActivity
import com.acme.services.camperservice.features.activityladder.params.AddActivityParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateAddActivity
import org.slf4j.LoggerFactory

internal class AddActivityAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(AddActivityAction::class.java)
    private val validate = ValidateAddActivity()

    fun execute(param: AddActivityParam): Result<LadderActivity, LadderError> {
        TODO("Implementation in PR 5c — service implementation")
    }
}
