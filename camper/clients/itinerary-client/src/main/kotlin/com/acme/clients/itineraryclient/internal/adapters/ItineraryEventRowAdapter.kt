package com.acme.clients.itineraryclient.internal.adapters

import com.acme.clients.itineraryclient.model.ItineraryEvent
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [ItineraryEvent] domain objects.
 */
object ItineraryEventRowAdapter {

    fun fromResultSet(rs: ResultSet): ItineraryEvent = ItineraryEvent(
        id = rs.getObject("id", UUID::class.java),
        itineraryId = rs.getObject("itinerary_id", UUID::class.java),
        title = rs.getString("title"),
        description = rs.getString("description"),
        details = rs.getString("details"),
        eventAt = rs.getTimestamp("event_at").toInstant(),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
