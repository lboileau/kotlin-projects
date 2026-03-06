package com.example.services.hello.features.world.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class WorldFixture(private val jdbcTemplate: JdbcTemplate) {

    fun createWorld(
        id: UUID = UUID.randomUUID(),
        name: String = "Test World",
        greeting: String = "Hello!",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO worlds (id, name, greeting, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
            id, name, greeting, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun truncate() {
        jdbcTemplate.execute("TRUNCATE TABLE worlds CASCADE")
    }
}
