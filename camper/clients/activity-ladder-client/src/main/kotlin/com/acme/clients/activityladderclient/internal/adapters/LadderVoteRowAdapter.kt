package com.acme.clients.activityladderclient.internal.adapters

import com.acme.clients.activityladderclient.model.LadderVote
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [LadderVote] domain objects.
 */
internal object LadderVoteRowAdapter {

    fun fromResultSet(rs: ResultSet): LadderVote = LadderVote(
        id = rs.getObject("id", UUID::class.java),
        ladderId = rs.getObject("ladder_id", UUID::class.java),
        roundNumber = rs.getInt("round_number"),
        userId = rs.getObject("user_id", UUID::class.java),
        votedForActivityId = rs.getObject("voted_for_activity_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
    )
}
