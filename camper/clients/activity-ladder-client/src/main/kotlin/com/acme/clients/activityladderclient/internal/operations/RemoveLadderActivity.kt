package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.RemoveLadderActivityParam
import com.acme.clients.activityladderclient.internal.validations.ValidateRemoveLadderActivity
import org.jdbi.v3.core.Jdbi

internal class RemoveLadderActivity(private val jdbi: Jdbi) {
    private val validate = ValidateRemoveLadderActivity()

    fun execute(param: RemoveLadderActivityParam): Result<Unit, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
