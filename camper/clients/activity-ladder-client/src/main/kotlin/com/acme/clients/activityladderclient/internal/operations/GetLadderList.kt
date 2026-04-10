package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.GetLadderListParam
import com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderList
import com.acme.clients.activityladderclient.model.Ladder
import org.jdbi.v3.core.Jdbi

internal class GetLadderList(private val jdbi: Jdbi) {
    private val validate = ValidateGetLadderList()

    fun execute(param: GetLadderListParam): Result<List<Ladder>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
