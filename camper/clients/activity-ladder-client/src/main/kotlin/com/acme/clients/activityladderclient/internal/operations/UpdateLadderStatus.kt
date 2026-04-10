package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.UpdateLadderStateParam
import com.acme.clients.activityladderclient.internal.validations.ValidateUpdateLadderState
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Jdbi

internal class UpdateLadderStatus(private val jdbi: Jdbi) {
    private val validate = ValidateUpdateLadderState()

    fun execute(param: UpdateLadderStateParam): Result<Ladder, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
