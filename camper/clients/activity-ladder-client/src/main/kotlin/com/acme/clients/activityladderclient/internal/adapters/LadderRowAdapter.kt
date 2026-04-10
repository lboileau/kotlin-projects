package com.acme.clients.activityladderclient.internal.adapters

import com.acme.clients.activityladderclient.model.Ladder
import com.acme.clients.activityladderclient.model.LadderStatus
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [Ladder] domain objects.
 * Handles nullable columns: currentRoundNumber, currentMatchActivityAId,
 * currentMatchActivityBId, winnerActivityId.
 */
internal object LadderRowAdapter {

    fun fromResultSet(rs: ResultSet): Ladder = Ladder(
        id = rs.getObject("id", UUID::class.java),
        creatorId = rs.getObject("creator_id", UUID::class.java),
        title = rs.getString("title"),
        status = LadderStatus.valueOf(rs.getString("status")),
        currentRoundNumber = rs.getObject("current_round_number") as Int?,
        currentMatchActivityAId = rs.getObject("current_match_activity_a_id", UUID::class.java),
        currentMatchActivityBId = rs.getObject("current_match_activity_b_id", UUID::class.java),
        isFinalRound = rs.getBoolean("is_final_round"),
        isGrandFinalReset = rs.getBoolean("is_grand_final_reset"),
        winnerActivityId = rs.getObject("winner_activity_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
