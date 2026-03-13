package com.acme.services.camperservice.features.plan.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class PlanFixture(private val jdbcTemplate: JdbcTemplate) {

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
        visibility: String = "private",
        ownerId: UUID,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO plans (id, name, visibility, owner_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            id, name, visibility, ownerId, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun insertPlanMember(
        planId: UUID,
        userId: UUID,
        role: String = "member",
        createdAt: Instant = Instant.now()
    ) {
        jdbcTemplate.update(
            "INSERT INTO plan_members (plan_id, user_id, role, created_at) VALUES (?, ?, ?, ?)",
            planId, userId, role, java.sql.Timestamp.from(createdAt)
        )
    }

    fun truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE plan_members, plans, users CASCADE")
    }
}
