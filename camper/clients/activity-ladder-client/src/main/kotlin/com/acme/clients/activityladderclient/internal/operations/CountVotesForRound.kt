package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.CountVotesForRoundParam
import com.acme.clients.activityladderclient.internal.validations.ValidateCountVotesForRound
import org.jdbi.v3.core.Jdbi

internal class CountVotesForRound(private val jdbi: Jdbi) {
    private val validate = ValidateCountVotesForRound()

    fun execute(param: CountVotesForRoundParam): Result<Int, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
