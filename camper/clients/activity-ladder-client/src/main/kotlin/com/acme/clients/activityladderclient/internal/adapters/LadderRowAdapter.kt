package com.acme.clients.activityladderclient.internal.adapters

import com.acme.clients.activityladderclient.model.Ladder
import java.sql.ResultSet

/**
 * Adapts database rows to [Ladder] domain objects.
 * Handles nullable columns: currentRoundNumber, currentMatchActivityAId,
 * currentMatchActivityBId, winnerActivityId.
 */
internal object LadderRowAdapter {

    fun fromResultSet(rs: ResultSet): Ladder {
        TODO("Implementation in PR 6 — client implementation")
    }
}
