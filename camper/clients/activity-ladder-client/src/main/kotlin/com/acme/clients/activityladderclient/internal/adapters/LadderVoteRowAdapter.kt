package com.acme.clients.activityladderclient.internal.adapters

import com.acme.clients.activityladderclient.model.LadderVote
import java.sql.ResultSet

/**
 * Adapts database rows to [LadderVote] domain objects.
 */
internal object LadderVoteRowAdapter {

    fun fromResultSet(rs: ResultSet): LadderVote {
        TODO("Implementation in PR 6 — client implementation")
    }
}
