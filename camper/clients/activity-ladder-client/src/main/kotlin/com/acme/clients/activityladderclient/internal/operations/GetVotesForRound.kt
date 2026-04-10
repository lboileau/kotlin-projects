package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.GetVotesForRoundParam
import com.acme.clients.activityladderclient.internal.validations.ValidateGetVotesForRound
import com.acme.clients.activityladderclient.model.LadderVote
import org.jdbi.v3.core.Jdbi

internal class GetVotesForRound(private val jdbi: Jdbi) {
    private val validate = ValidateGetVotesForRound()

    fun execute(param: GetVotesForRoundParam): Result<List<LadderVote>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
