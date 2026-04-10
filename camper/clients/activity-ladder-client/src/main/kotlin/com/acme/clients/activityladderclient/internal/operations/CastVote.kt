package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.CastVoteParam
import com.acme.clients.activityladderclient.internal.validations.ValidateCastVote
import com.acme.clients.activityladderclient.model.LadderVote
import org.jdbi.v3.core.Jdbi

internal class CastVote(private val jdbi: Jdbi) {
    private val validate = ValidateCastVote()

    fun execute(param: CastVoteParam): Result<LadderVote, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
