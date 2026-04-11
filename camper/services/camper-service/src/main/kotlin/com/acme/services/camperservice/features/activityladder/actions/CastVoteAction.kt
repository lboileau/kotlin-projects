package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.model.VoteOutcome
import com.acme.services.camperservice.features.activityladder.params.CastVoteParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateCastVote
import org.slf4j.LoggerFactory

internal class CastVoteAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(CastVoteAction::class.java)
    private val validate = ValidateCastVote()

    fun execute(param: CastVoteParam): Result<VoteOutcome, LadderError> {
        TODO("Implementation in PR 5c — service implementation")
    }
}
