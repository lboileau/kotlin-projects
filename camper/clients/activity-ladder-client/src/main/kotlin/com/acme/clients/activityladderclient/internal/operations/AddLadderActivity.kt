package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.AddLadderActivityParam
import com.acme.clients.activityladderclient.internal.validations.ValidateAddLadderActivity
import com.acme.clients.activityladderclient.model.LadderActivity
import org.jdbi.v3.core.Jdbi

internal class AddLadderActivity(private val jdbi: Jdbi) {
    private val validate = ValidateAddLadderActivity()

    fun execute(param: AddLadderActivityParam): Result<LadderActivity, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
