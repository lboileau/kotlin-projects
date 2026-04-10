package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.UpdateActivityLossAndBracketParam
import com.acme.clients.activityladderclient.internal.validations.ValidateUpdateActivityLossAndBracket
import com.acme.clients.activityladderclient.model.LadderActivity
import org.jdbi.v3.core.Jdbi

internal class UpdateActivityLossAndBracket(private val jdbi: Jdbi) {
    private val validate = ValidateUpdateActivityLossAndBracket()

    fun execute(param: UpdateActivityLossAndBracketParam): Result<LadderActivity, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
