package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.CreateLadderParam
import com.acme.clients.activityladderclient.internal.validations.ValidateCreateLadder
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Jdbi

internal class CreateLadder(private val jdbi: Jdbi) {
    private val validate = ValidateCreateLadder()

    fun execute(param: CreateLadderParam): Result<Ladder, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
