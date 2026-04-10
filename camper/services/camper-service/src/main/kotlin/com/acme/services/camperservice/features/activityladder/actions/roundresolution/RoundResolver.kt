package com.acme.services.camperservice.features.activityladder.actions.roundresolution

import com.acme.clients.activityladderclient.api.ActivityLadderClient
import com.acme.services.camperservice.features.activityladder.model.Ladder
import com.acme.services.camperservice.features.activityladder.model.VoteOutcome

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
        TODO("Implementation in PR 5c — service implementation")
    }
}
