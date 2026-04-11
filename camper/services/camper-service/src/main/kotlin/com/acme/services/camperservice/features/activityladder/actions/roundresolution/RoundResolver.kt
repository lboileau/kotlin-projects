package com.acme.services.camperservice.features.activityladder.actions.roundresolution

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.clients.activityladderclient.api.GetLadderActivitiesParam
import com.acme.clients.activityladderclient.api.GetVotesForRoundParam
import com.acme.clients.activityladderclient.api.SetLadderWinnerParam
import com.acme.clients.activityladderclient.api.UpdateActivityLossAndBracketParam
import com.acme.clients.activityladderclient.api.UpdateLadderStateParam
import com.acme.clients.activityladderclient.model.LadderBracket as ClientLadderBracket
import com.acme.clients.activityladderclient.model.LadderStatus as ClientLadderStatus
import com.acme.services.camperservice.features.activityladder.model.Ladder
import com.acme.services.camperservice.features.activityladder.model.VoteOutcome
import kotlin.random.Random
import java.util.UUID

/**
 * Pure class responsible for resolving the outcome of a round in an activity ladder.
 *
 * Precondition: the caller has established a FOR UPDATE lock on the ladder row
 * (via [ActivityLadderClient.withLadderLocked]). All operations on [client] within
 * [resolve] execute inside that transaction.
 *
 * The resolver tallies votes, handles ties, applies loss/bracket tracking,
 * detects Grand Final and Grand Final Reset scenarios, and returns the appropriate
 * [VoteOutcome] for the caller to publish as WebSocket events.
 */
class RoundResolver(private val client: ActivityLadderClient) {

    /**
     * Resolve the current round for the given ladder.
     *
     * @param ladder the current ladder state (fetched under the FOR UPDATE lock)
     * @param roundNumber the round number to resolve
     * @return the outcome of the round
     */
    fun resolve(ladder: Ladder, roundNumber: Int): VoteOutcome {
        val activityAId = requireNotNull(ladder.currentMatchActivityAId) { "currentMatchActivityAId must not be null during resolution" }
        val activityBId = requireNotNull(ladder.currentMatchActivityBId) { "currentMatchActivityBId must not be null during resolution" }

        val votes = client.getVotesForRound(GetVotesForRoundParam(ladder.id, roundNumber))
            .let { result ->
                when (result) {
                    is com.acme.clients.common.Result.Success -> result.value
                    is com.acme.clients.common.Result.Failure -> throw RuntimeException("Failed to fetch votes: ${result.error.message}")
                }
            }

        val tally = votes.groupingBy { it.votedForActivityId }.eachCount()
        val aCount = tally[activityAId] ?: 0
        val bCount = tally[activityBId] ?: 0
        val voteTotals: Map<UUID, Int> = mapOf(activityAId to aCount, activityBId to bCount)

        return if (aCount == bCount) {
            handleTie(ladder, roundNumber, voteTotals)
        } else {
            val (winnerId, loserId) = if (aCount > bCount) activityAId to activityBId else activityBId to activityAId
            handleWinDecision(ladder, roundNumber, winnerId, loserId, voteTotals)
        }
    }

    private fun handleTie(ladder: Ladder, roundNumber: Int, voteTotals: Map<UUID, Int>): VoteOutcome {
        // Tie: no losses recorded, re-select a pair from the current bracket pool
        val activities = fetchActivities(ladder.id)
        val nonEliminated = activities.filter { it.bracket != ClientLadderBracket.ELIMINATED }
        val (nextA, nextB) = selectPair(nonEliminated, ladder)
        val newRoundNumber = roundNumber + 1

        client.updateLadderState(
            UpdateLadderStateParam(
                ladderId = ladder.id,
                status = ClientLadderStatus.ACTIVE,
                currentRoundNumber = newRoundNumber,
                currentMatchActivityAId = nextA,
                currentMatchActivityBId = nextB,
                isFinalRound = ladder.isFinalRound,
                isGrandFinalReset = ladder.isGrandFinalReset,
            )
        )

        return VoteOutcome.RoundTied(
            voteCount = voteTotals.values.sum(),
            newRoundNumber = newRoundNumber,
            nextMatchAId = nextA,
            nextMatchBId = nextB,
            isFinalRound = ladder.isFinalRound,
            isGrandFinalReset = ladder.isGrandFinalReset,
        )
    }

    private fun handleWinDecision(
        ladder: Ladder,
        roundNumber: Int,
        winnerId: UUID,
        loserId: UUID,
        voteTotals: Map<UUID, Int>,
    ): VoteOutcome {
        // Fetch loser to get current losses
        val activities = fetchActivities(ladder.id)
        val loserActivity = activities.first { it.id == loserId }
        val newLosses = loserActivity.losses + 1
        val newBracket = if (newLosses == 1) ClientLadderBracket.LOSERS else ClientLadderBracket.ELIMINATED

        client.updateActivityLossAndBracket(
            UpdateActivityLossAndBracketParam(
                activityId = loserId,
                losses = newLosses,
                bracket = newBracket,
            )
        )

        // Re-fetch activities after mutation to get the updated bracket state
        val updatedActivities = fetchActivities(ladder.id)
        val nonEliminated = updatedActivities.filter { it.bracket != ClientLadderBracket.ELIMINATED }
        val winnersBracket = nonEliminated.filter { it.bracket == ClientLadderBracket.WINNERS }
        val losersBracket = nonEliminated.filter { it.bracket == ClientLadderBracket.LOSERS }

        // Check if tournament is complete
        if (nonEliminated.size == 1) {
            val winnerActivityId = nonEliminated[0].id
            client.setLadderWinner(SetLadderWinnerParam(ladderId = ladder.id, winnerActivityId = winnerActivityId))
            return VoteOutcome.LadderCompleted(winnerActivityId = winnerActivityId, voteTotals = voteTotals)
        }

        val newRoundNumber = roundNumber + 1

        // First Grand Final: exactly 1 activity in each bracket, not yet in final round
        if (winnersBracket.size == 1 && losersBracket.size == 1 && !ladder.isFinalRound) {
            val nextA = winnersBracket[0].id
            val nextB = losersBracket[0].id
            client.updateLadderState(
                UpdateLadderStateParam(
                    ladderId = ladder.id,
                    status = ClientLadderStatus.ACTIVE,
                    currentRoundNumber = newRoundNumber,
                    currentMatchActivityAId = nextA,
                    currentMatchActivityBId = nextB,
                    isFinalRound = true,
                    isGrandFinalReset = false,
                )
            )
            return VoteOutcome.RoundDecided(
                winnerActivityId = winnerId,
                voteTotals = voteTotals,
                newRoundNumber = newRoundNumber,
                nextMatchAId = nextA,
                nextMatchBId = nextB,
                isFinalRound = true,
                isGrandFinalReset = false,
            )
        }

        // Grand Final Reset: losers-bracket champion just won the first Grand Final,
        // giving the winners-bracket champion their first loss. Both survivors are now
        // in LOSERS — play the reset round with the same two activities.
        if (ladder.isFinalRound && !ladder.isGrandFinalReset && winnersBracket.isEmpty() && losersBracket.size == 2) {
            val nextA = losersBracket[0].id
            val nextB = losersBracket[1].id
            client.updateLadderState(
                UpdateLadderStateParam(
                    ladderId = ladder.id,
                    status = ClientLadderStatus.ACTIVE,
                    currentRoundNumber = newRoundNumber,
                    currentMatchActivityAId = nextA,
                    currentMatchActivityBId = nextB,
                    isFinalRound = true,
                    isGrandFinalReset = true,
                )
            )
            return VoteOutcome.RoundDecided(
                winnerActivityId = winnerId,
                voteTotals = voteTotals,
                newRoundNumber = newRoundNumber,
                nextMatchAId = nextA,
                nextMatchBId = nextB,
                isFinalRound = true,
                isGrandFinalReset = true,
            )
        }

        // Standard next-round bracket selection
        val pickFrom: List<com.acme.clients.activityladderclient.model.LadderActivity> = when {
            winnersBracket.size >= 2 && losersBracket.size >= 2 ->
                if (Random.Default.nextBoolean()) winnersBracket else losersBracket
            winnersBracket.size >= 2 -> winnersBracket
            losersBracket.size >= 2 -> losersBracket
            else ->
                // Edge case: one bracket has 1, other has ≥2 — play the larger bracket
                if (winnersBracket.size > losersBracket.size) winnersBracket else losersBracket
        }

        val (nextA, nextB) = selectPairFromList(pickFrom)
        client.updateLadderState(
            UpdateLadderStateParam(
                ladderId = ladder.id,
                status = ClientLadderStatus.ACTIVE,
                currentRoundNumber = newRoundNumber,
                currentMatchActivityAId = nextA,
                currentMatchActivityBId = nextB,
                isFinalRound = false,
                isGrandFinalReset = ladder.isGrandFinalReset,
            )
        )

        return VoteOutcome.RoundDecided(
            winnerActivityId = winnerId,
            voteTotals = voteTotals,
            newRoundNumber = newRoundNumber,
            nextMatchAId = nextA,
            nextMatchBId = nextB,
            isFinalRound = false,
            isGrandFinalReset = ladder.isGrandFinalReset,
        )
    }

    private fun fetchActivities(ladderId: UUID): List<com.acme.clients.activityladderclient.model.LadderActivity> {
        return client.getActivities(GetLadderActivitiesParam(ladderId))
            .let { result ->
                when (result) {
                    is com.acme.clients.common.Result.Success -> result.value
                    is com.acme.clients.common.Result.Failure -> throw RuntimeException("Failed to fetch activities: ${result.error.message}")
                }
            }
    }

    /**
     * Select two distinct random activities from the non-eliminated pool, respecting brackets.
     * Returns a pair of activity IDs.
     */
    private fun selectPair(
        nonEliminated: List<com.acme.clients.activityladderclient.model.LadderActivity>,
        ladder: Ladder,
    ): Pair<UUID, UUID> {
        val winnersBracket = nonEliminated.filter { it.bracket == ClientLadderBracket.WINNERS }
        val losersBracket = nonEliminated.filter { it.bracket == ClientLadderBracket.LOSERS }

        val pickFrom: List<com.acme.clients.activityladderclient.model.LadderActivity> = when {
            winnersBracket.size >= 2 && losersBracket.size >= 2 ->
                if (Random.Default.nextBoolean()) winnersBracket else losersBracket
            winnersBracket.size >= 2 -> winnersBracket
            losersBracket.size >= 2 -> losersBracket
            winnersBracket.size == 1 && losersBracket.size == 1 -> nonEliminated
            else ->
                if (winnersBracket.size > losersBracket.size) winnersBracket else losersBracket
        }

        return selectPairFromList(pickFrom)
    }

    private fun selectPairFromList(pool: List<com.acme.clients.activityladderclient.model.LadderActivity>): Pair<UUID, UUID> {
        require(pool.size >= 2) { "Need at least 2 activities to select a pair, got ${pool.size}" }
        val shuffled = pool.shuffled(Random.Default)
        return shuffled[0].id to shuffled[1].id
    }
}
