package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.CountVotesForRoundParam
import com.acme.clients.activityladderclient.api.GetLadderActivitiesParam
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.api.GetParticipantsParam
import com.acme.clients.activityladderclient.api.GetVotesForRoundParam
import com.acme.clients.common.Result
import com.acme.clients.userclient.api.GetByIdParam as UserGetByIdParam
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.mapper.ActivityLadderMapper
import com.acme.services.camperservice.features.activityladder.model.CurrentRound
import com.acme.services.camperservice.features.activityladder.model.EnrichedParticipant
import com.acme.services.camperservice.features.activityladder.model.LadderDetail
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.params.GetLadderDetailParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateGetLadderDetail
import org.slf4j.LoggerFactory

internal class GetLadderDetailAction(
    private val ladderClient: ActivityLadderClient,
    private val userClient: UserClient,
) {
    private val logger = LoggerFactory.getLogger(GetLadderDetailAction::class.java)
    private val validate = ValidateGetLadderDetail()

    fun execute(param: GetLadderDetailParam): Result<LadderDetail, LadderError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Fetching ladder detail ladderId={}", param.ladderId)

        val ladderResult = ladderClient.getLadderById(GetLadderByIdParam(param.ladderId))
        val clientLadder = when (ladderResult) {
            is Result.Success -> ladderResult.value
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(ladderResult.error))
        }
        val ladder = ActivityLadderMapper.fromClient(clientLadder)

        val activitiesResult = ladderClient.getActivities(GetLadderActivitiesParam(param.ladderId))
        val activities = when (activitiesResult) {
            is Result.Success -> activitiesResult.value.map { ActivityLadderMapper.fromClientActivity(it) }
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(activitiesResult.error))
        }

        val participantsResult = ladderClient.getParticipants(GetParticipantsParam(param.ladderId))
        val clientParticipants = when (participantsResult) {
            is Result.Success -> participantsResult.value
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(participantsResult.error))
        }

        val enrichedParticipants = clientParticipants.map { participant ->
            val userResult = userClient.getById(UserGetByIdParam(participant.userId))
            val user = when (userResult) {
                is Result.Success -> userResult.value
                is Result.Failure -> null
            }
            EnrichedParticipant(
                userId = participant.userId,
                username = user?.username,
                avatarSeed = user?.avatarSeed,
            )
        }

        val currentRound: CurrentRound? = if (ladder.status == LadderStatus.ACTIVE &&
            ladder.currentRoundNumber != null &&
            ladder.currentMatchActivityAId != null &&
            ladder.currentMatchActivityBId != null
        ) {
            val votesResult = ladderClient.getVotesForRound(
                GetVotesForRoundParam(ladderId = param.ladderId, roundNumber = ladder.currentRoundNumber)
            )
            val votes = when (votesResult) {
                is Result.Success -> votesResult.value
                is Result.Failure -> emptyList()
            }
            CurrentRound(
                roundNumber = ladder.currentRoundNumber,
                activityAId = ladder.currentMatchActivityAId,
                activityBId = ladder.currentMatchActivityBId,
                votesCast = votes.size,
                totalVoters = clientParticipants.size,
                votedUserIds = votes.map { it.userId },
            )
        } else {
            null
        }

        return Result.Success(
            LadderDetail(
                ladder = ladder,
                activities = activities,
                participants = enrichedParticipants,
                currentRound = currentRound,
            )
        )
    }
}
