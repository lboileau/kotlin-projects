package com.acme.clients.itineraryclient.internal.adapters

import com.acme.clients.itineraryclient.model.Itinerary
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [Itinerary] domain objects.
 */
object ItineraryRowAdapter {

    fun fromResultSet(rs: ResultSet): Itinerary = Itinerary(
        id = rs.getObject("id", UUID::class.java),
        planId = rs.getObject("plan_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
