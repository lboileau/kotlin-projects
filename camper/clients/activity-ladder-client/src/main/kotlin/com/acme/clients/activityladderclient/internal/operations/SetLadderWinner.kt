package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.SetLadderWinnerParam
import com.acme.clients.activityladderclient.internal.validations.ValidateSetLadderWinner
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Jdbi

internal class SetLadderWinner(private val jdbi: Jdbi) {
    private val validate = ValidateSetLadderWinner()

    fun execute(param: SetLadderWinnerParam): Result<Ladder, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
