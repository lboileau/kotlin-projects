package com.acme.services.camperservice.features.activityladder.model

import java.util.UUID

/**
 * Sealed class representing the outcome of casting a vote.
 * Returned by [com.acme.services.camperservice.features.activityladder.actions.CastVoteAction]
 * and consumed by the controller to publish the correct sequence of WebSocket events.
 */
sealed class VoteOutcome {

    /** Vote was recorded but the round is not yet complete (more voters remain). */
    data class VoteRecorded(
        val voteCount: Int,
        val votersRemaining: Int,
        val voterId: UUID,
    ) : VoteOutcome()

    /** All votes are in and the round ended in a tie — round number advances, new pair selected. */
    data class RoundTied(
        val voteCount: Int,
        val newRoundNumber: Int,
        val nextMatchAId: UUID,
        val nextMatchBId: UUID,
        val isFinalRound: Boolean,
        val isGrandFinalReset: Boolean,
    ) : VoteOutcome()

    /** All votes are in and one activity won — proceed to next round. */
    data class RoundDecided(
        val winnerActivityId: UUID,
        val voteTotals: Map<UUID, Int>,
        val newRoundNumber: Int,
        val nextMatchAId: UUID,
        val nextMatchBId: UUID,
        val isFinalRound: Boolean,
        val isGrandFinalReset: Boolean,
    ) : VoteOutcome()

    /** All votes in, tournament is complete — one activity won the whole ladder. */
    data class LadderCompleted(
        val winnerActivityId: UUID,
        val voteTotals: Map<UUID, Int>,
    ) : VoteOutcome()
}
