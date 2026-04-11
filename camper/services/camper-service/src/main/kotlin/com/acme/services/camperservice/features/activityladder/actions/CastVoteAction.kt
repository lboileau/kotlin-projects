package com.acme.services.camperservice.features.activityladder.actions

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.CastVoteParam as ClientCastVoteParam
import com.acme.clients.activityladderclient.api.CountVotesForRoundParam
import com.acme.clients.activityladderclient.api.GetLadderByIdParam
import com.acme.clients.activityladderclient.api.GetParticipantsParam
import com.acme.clients.activityladderclient.api.GetVotesForRoundParam
import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.services.camperservice.features.activityladder.actions.roundresolution.RoundResolver
import com.acme.services.camperservice.features.activityladder.error.LadderError
import com.acme.services.camperservice.features.activityladder.mapper.ActivityLadderMapper
import com.acme.services.camperservice.features.activityladder.model.LadderStatus
import com.acme.services.camperservice.features.activityladder.model.VoteOutcome
import com.acme.services.camperservice.features.activityladder.params.CastVoteParam
import com.acme.services.camperservice.features.activityladder.validations.ValidateCastVote
import org.slf4j.LoggerFactory

internal class CastVoteAction(
    private val ladderClient: ActivityLadderClient,
) {
    private val logger = LoggerFactory.getLogger(CastVoteAction::class.java)
    private val validate = ValidateCastVote()

    fun execute(param: CastVoteParam): Result<VoteOutcome, LadderError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Casting vote ladderId={} userId={} votedFor={}", param.ladderId, param.requestingUserId, param.votedForActivityId)

        // Preload state for cheap checks before grabbing the lock
        val preLadderResult = ladderClient.getLadderById(GetLadderByIdParam(param.ladderId))
        val preLadder = when (preLadderResult) {
            is Result.Success -> ActivityLadderMapper.fromClient(preLadderResult.value)
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(preLadderResult.error))
        }

        if (preLadder.status != LadderStatus.ACTIVE) {
            return Result.Failure(LadderError.IllegalState("ACTIVE", preLadder.status.name))
        }

        // Check voter eligibility
        val preParticipantsResult = ladderClient.getParticipants(GetParticipantsParam(param.ladderId))
        val preParticipants = when (preParticipantsResult) {
            is Result.Success -> preParticipantsResult.value
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(preParticipantsResult.error))
        }
        if (preParticipants.none { it.userId == param.requestingUserId }) {
            return Result.Failure(LadderError.NotEligibleVoter(param.requestingUserId))
        }

        // Check vote target is one of the current match activities
        if (param.votedForActivityId != preLadder.currentMatchActivityAId &&
            param.votedForActivityId != preLadder.currentMatchActivityBId
        ) {
            return Result.Failure(LadderError.InvalidVoteTarget(param.votedForActivityId))
        }

        // Check not already voted this round
        val roundNumber = requireNotNull(preLadder.currentRoundNumber) { "currentRoundNumber must not be null for ACTIVE ladder" }
        val preVotesResult = ladderClient.getVotesForRound(GetVotesForRoundParam(param.ladderId, roundNumber))
        val preVotes = when (preVotesResult) {
            is Result.Success -> preVotesResult.value
            is Result.Failure -> return Result.Failure(LadderError.fromClientError(preVotesResult.error))
        }
        if (preVotes.any { it.userId == param.requestingUserId }) {
            return Result.Failure(LadderError.AlreadyVoted(param.requestingUserId, roundNumber))
        }

        // Everything inside the lock: cast vote, count, possibly resolve round
        val lockedResult: Result<VoteOutcome, com.acme.clients.common.error.AppError> =
            ladderClient.withLadderLocked(param.ladderId) { scoped ->
                // Re-fetch ladder under lock
                val lockedLadderResult = scoped.getLadderById(GetLadderByIdParam(param.ladderId))
                val lockedLadder = when (lockedLadderResult) {
                    is Result.Success -> ActivityLadderMapper.fromClient(lockedLadderResult.value)
                    is Result.Failure -> return@withLadderLocked lockedLadderResult
                }

                // Re-check status under lock
                if (lockedLadder.status != LadderStatus.ACTIVE) {
                    return@withLadderLocked Result.Failure(
                        com.acme.clients.common.error.ValidationError("status", "ladder is no longer ACTIVE")
                    )
                }

                val lockedRoundNumber = requireNotNull(lockedLadder.currentRoundNumber)

                // Re-check vote target (round may have advanced)
                if (param.votedForActivityId != lockedLadder.currentMatchActivityAId &&
                    param.votedForActivityId != lockedLadder.currentMatchActivityBId
                ) {
                    return@withLadderLocked Result.Failure(
                        com.acme.clients.common.error.ValidationError("votedForActivityId", "activity is not in the current match")
                    )
                }

                // Re-check duplicate vote under lock
                val existingVotesResult = scoped.getVotesForRound(GetVotesForRoundParam(param.ladderId, lockedRoundNumber))
                val existingVotes = when (existingVotesResult) {
                    is Result.Success -> existingVotesResult.value
                    is Result.Failure -> return@withLadderLocked existingVotesResult
                }
                if (existingVotes.any { it.userId == param.requestingUserId }) {
                    return@withLadderLocked Result.Failure(ConflictError("vote", "user already voted this round"))
                }

                // Cast the vote
                val castResult = scoped.castVote(
                    ClientCastVoteParam(
                        ladderId = param.ladderId,
                        roundNumber = lockedRoundNumber,
                        userId = param.requestingUserId,
                        votedForActivityId = param.votedForActivityId,
                    )
                )
                when (castResult) {
                    is Result.Failure -> return@withLadderLocked castResult
                    is Result.Success -> { /* continue */ }
                }

                // Count votes after cast
                val countResult = scoped.countVotesForRound(CountVotesForRoundParam(param.ladderId, lockedRoundNumber))
                val votesCast = when (countResult) {
                    is Result.Success -> countResult.value
                    is Result.Failure -> return@withLadderLocked countResult
                }

                val totalVoters = preParticipants.size
                val remaining = totalVoters - votesCast

                if (remaining > 0) {
                    // Round not complete yet
                    return@withLadderLocked Result.Success(
                        VoteOutcome.VoteRecorded(
                            voteCount = votesCast,
                            votersRemaining = remaining,
                            voterId = param.requestingUserId,
                        )
                    )
                }

                // All votes in — resolve the round
                val resolver = RoundResolver(scoped)
                val outcome = resolver.resolve(lockedLadder, lockedRoundNumber)
                Result.Success(outcome)
            }

        // Map client error to LadderError
        return when (lockedResult) {
            is Result.Success -> {
                val outcome = lockedResult.value
                // Map validation errors from within the lock to LadderError
                Result.Success(outcome)
            }
            is Result.Failure -> {
                val err = lockedResult.error
                when {
                    err is ConflictError && err.detail.contains("already voted") ->
                        Result.Failure(LadderError.AlreadyVoted(param.requestingUserId, roundNumber))
                    err is com.acme.clients.common.error.ValidationError && err.field == "votedForActivityId" ->
                        Result.Failure(LadderError.InvalidVoteTarget(param.votedForActivityId))
                    err is com.acme.clients.common.error.ValidationError && err.field == "status" ->
                        Result.Failure(LadderError.IllegalState("ACTIVE", "ladder state changed"))
                    else -> Result.Failure(LadderError.fromClientError(err))
                }
            }
        }
    }
}
