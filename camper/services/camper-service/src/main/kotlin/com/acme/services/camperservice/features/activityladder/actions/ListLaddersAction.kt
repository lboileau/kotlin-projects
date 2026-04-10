package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.GetLadderActivitiesParam
import com.acme.clients.activityladderclient.api.GetLadderListParam
import com.acme.clients.activityladderclient.api.GetParticipantsParam
import com.acme.clients.common.Result
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.mapper.ActivityLadderMapper
import com.acme.services.camperservice.features.activityladder.model.LadderSummary
import com.acme.services.camperservice.features.activityladder.params.ListLaddersParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateListLadders
import org.slf4j.LoggerFactory

internal class ListLaddersAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(ListLaddersAction::class.java)
    private val validate = ValidateListLadders()

    fun execute(param: ListLaddersParam): Result<List<LadderSummary>, LadderError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Listing ladders for userId={}", param.requestingUserId)

        val laddersResult = ladderClient.getLadderList(GetLadderListParam())
        val ladders = when (laddersResult) {
            is Result.Success -> laddersResult.value
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(laddersResult.error))
        }

        // N+1 per plan Decision 9 — simple for MVP, list is small
        val summaries = ladders.map { ladder ->
            val activityCount = ladderClient.getActivities(GetLadderActivitiesParam(ladder.id))
                .let { result -> when (result) {
                    is Result.Success -> result.value.size
                    is Result.Failure -> 0
                }}
            val participantCount = ladderClient.getParticipants(GetParticipantsParam(ladder.id))
                .let { result -> when (result) {
                    is Result.Success -> result.value.size
                    is Result.Failure -> 0
                }}
            LadderSummary(
                ladder = ActivityLadderMapper.fromClient(ladder),
                activityCount = activityCount,
                participantCount = participantCount,
            )
        }

        return Result.Success(summaries)
    }
}
