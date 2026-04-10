package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.RestartLadderParam
import com.acme.clients.activityladderclient.internal.validations.ValidateRestartLadder
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Jdbi

internal class RestartLadder(private val jdbi: Jdbi) {
    private val validate = ValidateRestartLadder()

    fun execute(param: RestartLadderParam): Result<Ladder, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
