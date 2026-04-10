package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.BulkInsertParticipantsParam
import com.acme.clients.activityladderclient.internal.validations.ValidateBulkInsertParticipants
import com.acme.clients.activityladderclient.model.LadderParticipant
import org.jdbi.v3.core.Jdbi

internal class BulkInsertParticipants(private val jdbi: Jdbi) {
    private val validate = ValidateBulkInsertParticipants()

    fun execute(param: BulkInsertParticipantsParam): Result<List<LadderParticipant>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
