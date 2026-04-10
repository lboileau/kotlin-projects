package com.acme.clients.activityladderclient.internal.adapters

import com.acme.clients.activityladderclient.model.LadderActivity
import com.acme.clients.activityladderclient.model.LadderBracket
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [LadderActivity] domain objects.
 */
internal object LadderActivityRowAdapter {

    fun fromResultSet(rs: ResultSet): LadderActivity = LadderActivity(
        id = rs.getObject("id", UUID::class.java),
        ladderId = rs.getObject("ladder_id", UUID::class.java),
        name = rs.getString("name"),
        imageUrl = rs.getString("image_url"),
        distanceMinutes = rs.getInt("distance_minutes"),
        costPerPerson = rs.getBigDecimal("cost_per_person"),
        losses = rs.getInt("losses"),
        bracket = LadderBracket.valueOf(rs.getString("bracket")),
        displayOrder = rs.getInt("display_order"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
    )
}
