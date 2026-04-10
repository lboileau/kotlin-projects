package com.acme.clients.activityladderclient.internal.adapters

import com.acme.clients.activityladderclient.model.LadderActivity
import java.sql.ResultSet

/**
 * Adapts database rows to [LadderActivity] domain objects.
 */
internal object LadderActivityRowAdapter {

    fun fromResultSet(rs: ResultSet): LadderActivity {
        TODO("Implementation in PR 6 — client implementation")
    }
}
