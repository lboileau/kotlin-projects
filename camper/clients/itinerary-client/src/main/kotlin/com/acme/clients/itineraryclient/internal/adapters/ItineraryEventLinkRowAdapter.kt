package com.acme.clients.itineraryclient.internal.adapters

import com.acme.clients.itineraryclient.model.ItineraryEventLink
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [ItineraryEventLink] domain objects.
 */
object ItineraryEventLinkRowAdapter {

    fun fromResultSet(rs: ResultSet): ItineraryEventLink = ItineraryEventLink(
        id = rs.getObject("id", UUID::class.java),
        eventId = rs.getObject("event_id", UUID::class.java),
        url = rs.getString("url"),
        label = rs.getString("label"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
