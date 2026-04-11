package com.acme.clients.activityladderclient.internal.adapters

import com.acme.clients.activityladderclient.model.LadderParticipant
import java.sql.ResultSet

/**
 * Adapts database rows to [LadderParticipant] domain objects.
 */
internal object LadderParticipantRowAdapter {

    fun fromResultSet(rs: ResultSet): LadderParticipant {
        TODO("Implementation in PR 6 — client implementation")
    }
}
