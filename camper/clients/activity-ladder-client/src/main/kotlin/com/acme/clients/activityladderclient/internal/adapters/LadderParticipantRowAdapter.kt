package com.acme.clients.activityladderclient.internal.adapters

import com.acme.clients.activityladderclient.model.LadderParticipant
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [LadderParticipant] domain objects.
 */
internal object LadderParticipantRowAdapter {

    fun fromResultSet(rs: ResultSet): LadderParticipant = LadderParticipant(
        id = rs.getObject("id", UUID::class.java),
        ladderId = rs.getObject("ladder_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
    )
}
