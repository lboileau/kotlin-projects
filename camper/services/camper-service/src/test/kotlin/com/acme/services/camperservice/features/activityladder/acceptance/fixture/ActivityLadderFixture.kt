package com.acme.services.camperservice.features.activityladder.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class ActivityLadderFixture(private val jdbcTemplate: JdbcTemplate) {

    companion object {
        val KNOWN_LADDER_ID_1: UUID = UUID.fromString("ee000000-0001-4000-8000-000000000001")
        val KNOWN_LADDER_ID_2: UUID = UUID.fromString("ee000000-0002-4000-8000-000000000002")
        val KNOWN_ACTIVITY_ID_1: UUID = UUID.fromString("ff000000-0001-4000-8000-000000000001")
        val KNOWN_ACTIVITY_ID_2: UUID = UUID.fromString("ff000000-0002-4000-8000-000000000002")
        val KNOWN_ACTIVITY_ID_3: UUID = UUID.fromString("ff000000-0003-4000-8000-000000000003")
    }

    fun truncateAll() {
        // Child tables first to avoid FK violations; CASCADE handles any remaining dependencies.
        jdbcTemplate.execute(
            "TRUNCATE TABLE ladder_votes, ladder_participants, ladder_activities, activity_ladders, users CASCADE"
        )
    }

    fun insertUser(
        id: UUID = UUID.randomUUID(),
        email: String = "user-${UUID.randomUUID().toString().take(8)}@example.com",
        username: String? = null,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO users (id, email, username, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
            id, email, username,
            Timestamp.from(createdAt),
            Timestamp.from(updatedAt),
        )
        return id
    }

    fun insertLadder(
        id: UUID = UUID.randomUUID(),
        creatorId: UUID,
        title: String = "Test Ladder",
        status: String = "DRAFT",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO activity_ladders (id, creator_id, title, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, creatorId, title, status,
            Timestamp.from(createdAt),
            Timestamp.from(updatedAt),
        )
        return id
    }

    fun insertActivity(
        id: UUID = UUID.randomUUID(),
        ladderId: UUID,
        name: String = "Activity-${UUID.randomUUID().toString().take(8)}",
        imageUrl: String = "https://example.com/activity.jpg",
        distanceMinutes: Int = 60,
        costPerPerson: BigDecimal = BigDecimal("10.00"),
        losses: Int = 0,
        bracket: String = "WINNERS",
        displayOrder: Int = 1,
        createdAt: Instant = Instant.now(),
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO ladder_activities (id, ladder_id, name, image_url, distance_minutes, cost_per_person, losses, bracket, display_order, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, ladderId, name, imageUrl, distanceMinutes, costPerPerson, losses, bracket, displayOrder,
            Timestamp.from(createdAt),
        )
        return id
    }

    fun insertParticipant(
        id: UUID = UUID.randomUUID(),
        ladderId: UUID,
        userId: UUID,
        createdAt: Instant = Instant.now(),
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO ladder_participants (id, ladder_id, user_id, created_at) VALUES (?, ?, ?, ?)",
            id, ladderId, userId,
            Timestamp.from(createdAt),
        )
        return id
    }

    fun insertVote(
        id: UUID = UUID.randomUUID(),
        ladderId: UUID,
        roundNumber: Int,
        userId: UUID,
        votedForActivityId: UUID,
        createdAt: Instant = Instant.now(),
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO ladder_votes (id, ladder_id, round_number, user_id, voted_for_activity_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, ladderId, roundNumber, userId, votedForActivityId,
            Timestamp.from(createdAt),
        )
        return id
    }
}
