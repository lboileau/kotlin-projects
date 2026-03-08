package com.acme.services.camperservice.features.assignment.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class AssignmentFixture(private val jdbcTemplate: JdbcTemplate) {

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
        createdAt: Instant = Instant.now()
    ) {
        jdbcTemplate.update(
            "INSERT INTO plan_members (plan_id, user_id, created_at) VALUES (?, ?, ?)",
            planId, userId, java.sql.Timestamp.from(createdAt)
        )
    }

    fun createAssignment(
        id: UUID = UUID.randomUUID(),
        planId: UUID,
        name: String = "Assignment-${UUID.randomUUID().toString().take(8)}",
        type: String = "tent",
        maxOccupancy: Int = 4,
        ownerId: UUID,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO assignments (id, plan_id, name, type, max_occupancy, owner_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, planId, name, type, maxOccupancy, ownerId,
            java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun addMember(
        assignmentId: UUID,
        userId: UUID,
        planId: UUID,
        type: String = "tent",
        createdAt: Instant = Instant.now()
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO assignment_members (assignment_id, user_id, plan_id, assignment_type, created_at)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            assignmentId, userId, planId, type, java.sql.Timestamp.from(createdAt)
        )
    }

    fun truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE assignment_members, assignments, plan_members, plans, users CASCADE")
    }
}
