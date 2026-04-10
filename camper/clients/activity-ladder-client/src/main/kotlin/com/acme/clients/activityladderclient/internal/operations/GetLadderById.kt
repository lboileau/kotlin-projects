package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderById
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Jdbi

internal class GetLadderById(private val jdbi: Jdbi) {
    private val validate = ValidateGetLadderById()

    fun execute(param: GetLadderByIdParam): Result<Ladder, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
