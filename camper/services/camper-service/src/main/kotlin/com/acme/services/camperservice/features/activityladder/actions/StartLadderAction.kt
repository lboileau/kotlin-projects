package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.BulkInsertParticipantsParam
import com.acme.clients.activityladderclient.api.GetLadderActivitiesParam
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.api.UpdateLadderStateParam
import com.acme.clients.activityladderclient.model.LadderStatus as ClientLadderStatus
import com.acme.clients.common.Result
import com.acme.clients.common.error.ValidationError
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.mapper.ActivityLadderMapper
import com.acme.services.camperservice.features.activityladder.model.EnrichedParticipant
import com.acme.services.camperservice.features.activityladder.model.LadderDetail
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.params.StartLadderParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateStartLadder
import com.acme.services.camperservice.websocket.LadderEventPublisher
import kotlin.random.Random
import org.slf4j.LoggerFactory
import java.util.UUID

internal class StartLadderAction(
    private val ladderClient: ActivityLadderClient,
    private val eventPublisher: LadderEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(StartLadderAction::class.java)
    private val validate = ValidateStartLadder()

    private data class StartResult(
        val updatedLadder: com.acme.services.camperservice.features.activityladder.model.Ladder,
        val activities: List<com.acme.clients.activityladderclient.model.LadderActivity>,
        val clientParticipants: List<com.acme.clients.activityladderclient.model.LadderParticipant>,
        val firstA: UUID,
        val firstB: UUID,
    )

    fun execute(param: StartLadderParam): Result<LadderDetail, LadderError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Starting ladder ladderId={} userId={}", param.ladderId, param.requestingUserId)

        // Pre-lock cheap checks for fast-fail on the common (non-concurrent) path
        val preLadderResult = ladderClient.getLadderById(GetLadderByIdParam(param.ladderId))
        val preLadder = when (preLadderResult) {
            is Result.Success -> ActivityLadderMapper.fromClient(preLadderResult.value)
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(preLadderResult.error))
        }

        if (preLadder.status != LadderStatus.DRAFT) {
            return Result.Failure(LadderError.IllegalState("DRAFT", preLadder.status.name))
        }
        if (preLadder.creatorId != param.requestingUserId) {
            return Result.Failure(LadderError.NotCreator(param.ladderId, param.requestingUserId))
        }
        if (param.presentUserIds.isEmpty()) {
            return Result.Failure(LadderError.NoPresentUsers(param.ladderId))
        }

        // Authoritative sequence under the row lock: re-check DRAFT, fetch activities, insert
        // participants, select first pair, and update ladder state atomically.
        val lockedResult: Result<StartResult, com.acme.clients.common.error.AppError> =
            ladderClient.withLadderLocked(param.ladderId) { scoped ->
                // Re-fetch under lock — another Start might have won the race
                val lockedLadderResult = scoped.getLadderById(GetLadderByIdParam(param.ladderId))
                val lockedLadder = when (lockedLadderResult) {
                    is Result.Success -> ActivityLadderMapper.fromClient(lockedLadderResult.value)
                    is Result.Failure -> return@withLadderLocked lockedLadderResult
                }

                if (lockedLadder.status != LadderStatus.DRAFT) {
                    return@withLadderLocked Result.Failure(
                        ValidationError("status", "ladder is no longer in DRAFT")
                    )
                }

                // Fetch activities inside the lock for the pair selection
                val activitiesResult = scoped.getActivities(GetLadderActivitiesParam(param.ladderId))
                val activities = when (activitiesResult) {
                    is Result.Success -> activitiesResult.value
                    is Result.Failure -> return@withLadderLocked activitiesResult
                }

                if (activities.size < 2) {
                    return@withLadderLocked Result.Failure(
                        ValidationError("activities", "need at least 2 activities to start")
                    )
                }

                // Snapshot presence set as frozen participants
                val participantsResult = scoped.bulkInsertParticipants(
                    BulkInsertParticipantsParam(ladderId = param.ladderId, userIds = param.presentUserIds)
                )
                val clientParticipants = when (participantsResult) {
                    is Result.Success -> participantsResult.value
                    is Result.Failure -> return@withLadderLocked participantsResult
                }

                // Select first random pair and update ladder state to ACTIVE
                val shuffled = activities.shuffled(Random.Default)
                val firstA = shuffled[0].id
                val firstB = shuffled[1].id

                val updateResult = scoped.updateLadderState(
                    UpdateLadderStateParam(
                        ladderId = param.ladderId,
                        status = ClientLadderStatus.ACTIVE,
                        currentRoundNumber = 1,
                        currentMatchActivityAId = firstA,
                        currentMatchActivityBId = firstB,
                        isFinalRound = false,
                        isGrandFinalReset = false,
                    )
                )
                val updatedClientLadder = when (updateResult) {
                    is Result.Success -> updateResult.value
                    is Result.Failure -> return@withLadderLocked updateResult
                }

                Result.Success(
                    StartResult(
                        updatedLadder = ActivityLadderMapper.fromClient(updatedClientLadder),
                        activities = activities,
                        clientParticipants = clientParticipants,
                        firstA = firstA,
                        firstB = firstB,
                    )
                )
            }

        val startResult = when (lockedResult) {
            is Result.Success -> lockedResult.value
            is Result.Failure -> {
                val err = lockedResult.error
                return when {
                    err is ValidationError && err.field == "status" ->
                        Result.Failure(LadderError.IllegalState("DRAFT", "ladder state changed"))
                    err is ValidationError && err.field == "activities" ->
                        Result.Failure(LadderError.NotEnoughActivities(0))
                    else -> Result.Failure(LadderError.fromClientError(err))
                }
            }
        }

        // Publish events outside the lock
        eventPublisher.started(param.ladderId)
        eventPublisher.roundStarted(
            ladderId = param.ladderId,
            roundNumber = 1,
            activityAId = startResult.firstA,
            activityBId = startResult.firstB,
            isFinal = false,
            isReset = false,
        )

        val enrichedParticipants = startResult.clientParticipants.map { p ->
            EnrichedParticipant(userId = p.userId, username = null, avatarSeed = null)
        }

        return Result.Success(
            LadderDetail(
                ladder = startResult.updatedLadder,
                activities = startResult.activities.map { ActivityLadderMapper.fromClientActivity(it) },
                participants = enrichedParticipants,
                currentRound = null,
            )
        )
    }
}
