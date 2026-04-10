package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.GetParticipantsParam
import com.acme.clients.activityladderclient.internal.validations.ValidateGetParticipants
import com.acme.clients.activityladderclient.model.LadderParticipant
import org.jdbi.v3.core.Jdbi

internal class GetParticipants(private val jdbi: Jdbi) {
    private val validate = ValidateGetParticipants()

    fun execute(param: GetParticipantsParam): Result<List<LadderParticipant>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
