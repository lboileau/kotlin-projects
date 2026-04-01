package com.acme.services.camperservice.features.item.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class ItemFixture(private val jdbcTemplate: JdbcTemplate) {

    companion object {
        /** Seeded by V036 migration — always present after migrations run. */
        val COOKING_EQUIPMENT_PACK_ID: UUID = UUID.fromString("cc000000-0001-4000-8000-000000000001")
    }

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
        gearPackId: UUID? = null,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO items (id, plan_id, user_id, name, category, quantity, packed, gear_pack_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, planId, userId, name, category, quantity, packed, gearPackId, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(updatedAt)
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
        jdbcTemplate.execute("TRUNCATE TABLE items, plan_members, plans, users CASCADE")
    }
}
