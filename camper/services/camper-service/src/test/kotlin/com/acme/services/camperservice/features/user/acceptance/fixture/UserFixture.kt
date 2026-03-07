package com.acme.services.camperservice.features.user.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class UserFixture(private val jdbcTemplate: JdbcTemplate) {

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

    fun truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE plan_members, plans, users CASCADE")
    }

    companion object {
        val KNOWN_USER_ID: UUID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
    }
}
