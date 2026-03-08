package com.acme.services.camperservice.features.item.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class ItemFixture(private val jdbcTemplate: JdbcTemplate) {

    fun insertUser(
        id: UUID = UUID.randomUUID(),
        email: String = "user-${UUID.randomUUID().toString().take(8)}@example.com",
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO users (id, email, created_at, updated_at) VALUES (?, ?, ?, ?)",
            id, email, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
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
            "INSERT INTO plans (id, name, owner_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
            id, name, ownerId, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun insertItem(
        id: UUID = UUID.randomUUID(),
        planId: UUID? = null,
        userId: UUID? = null,
        name: String = "Item-${UUID.randomUUID().toString().take(8)}",
        category: String = "gear",
        quantity: Int = 1,
        packed: Boolean = false,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO items (id, plan_id, user_id, name, category, quantity, packed, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, planId, userId, name, category, quantity, packed, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE items, plan_members, plans, users CASCADE")
    }
}
