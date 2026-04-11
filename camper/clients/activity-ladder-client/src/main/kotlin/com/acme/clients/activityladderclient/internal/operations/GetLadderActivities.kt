package com.acme.clients.activityladderclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.activityladderclient.api.GetLadderActivitiesParam
import com.acme.clients.activityladderclient.internal.validations.ValidateGetLadderActivities
import com.acme.clients.activityladderclient.model.LadderActivity
import org.jdbi.v3.core.Jdbi

internal class GetLadderActivities(private val jdbi: Jdbi) {
    private val validate = ValidateGetLadderActivities()

    fun execute(param: GetLadderActivitiesParam): Result<List<LadderActivity>, AppError> {
        TODO("Implementation in PR 6 — client implementation")
    }
}
