package com.acme.services.camperservice.features.itinerary.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class ItineraryFixture(private val jdbcTemplate: JdbcTemplate) {

    fun insertUser(
        id: UUID = UUID.randomUUID(),
        email: String = "user-${UUID.randomUUID().toString().take(8)}@example.com",
        username: String? = "user-${UUID.randomUUID().toString().take(8)}",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO users (id, email, username, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
            id, email, username, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun insertPlan(
        id: UUID = UUID.randomUUID(),
        name: String = "Plan-${UUID.randomUUID().toString().take(8)}",
        ownerId: UUID,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO plans (id, name, visibility, owner_id, created_at, updated_at) VALUES (?, ?, 'private', ?, ?, ?)",
            id, name, ownerId, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun insertItinerary(
        id: UUID = UUID.randomUUID(),
        planId: UUID,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO itineraries (id, plan_id, created_at, updated_at) VALUES (?, ?, ?, ?)",
            id, planId, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun insertEvent(
        id: UUID = UUID.randomUUID(),
        itineraryId: UUID,
        title: String = "Event-${UUID.randomUUID().toString().take(8)}",
        description: String? = null,
        details: String? = null,
        eventAt: Instant = Instant.now(),
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO itinerary_events (id, itinerary_id, title, description, details, event_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, itineraryId, title, description, details,
            java.sql.Timestamp.from(eventAt), java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE itinerary_events, itineraries, plan_members, plans, users CASCADE")
    }
}
