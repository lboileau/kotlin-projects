package com.acme.services.camperservice.features.gearsync.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class GearSyncFixture(private val jdbcTemplate: JdbcTemplate) {

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

    fun insertAssignment(
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

    fun insertAssignmentMember(
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

    fun insertItem(
        id: UUID = UUID.randomUUID(),
        planId: UUID? = null,
        userId: UUID? = null,
        name: String,
        category: String,
        quantity: Int = 1,
        packed: Boolean = false,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO items (id, plan_id, user_id, name, category, quantity, packed, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, planId, userId, name, category, quantity, packed,
            java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
        )
        return id
    }

    fun getItemsByPlanId(planId: UUID): List<Map<String, Any>> {
        return jdbcTemplate.queryForList(
            "SELECT name, category, quantity FROM items WHERE plan_id = ? ORDER BY name",
            planId
        )
    }

    fun truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE items, assignment_members, assignments, plan_members, plans, users CASCADE")
    }
}
