package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.model.LadderDetail
import com.acme.services.camperservice.features.activityladder.params.GetLadderDetailParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateGetLadderDetail
import org.slf4j.LoggerFactory

internal class GetLadderDetailAction(
    private val ladderClient: ActivityLadderClient,
    private val userClient: UserClient,
) {
    private val logger = LoggerFactory.getLogger(GetLadderDetailAction::class.java)
    private val validate = ValidateGetLadderDetail()

    fun execute(param: GetLadderDetailParam): Result<LadderDetail, LadderError> {
        TODO("Implementation in PR 5c — service implementation")
    }
}
