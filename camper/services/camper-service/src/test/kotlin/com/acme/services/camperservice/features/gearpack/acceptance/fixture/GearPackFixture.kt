package com.acme.services.camperservice.features.gearpack.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

class GearPackFixture(private val jdbcTemplate: JdbcTemplate) {

    companion object {
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

    fun getItemsByPlanId(planId: UUID): List<Map<String, Any?>> {
        return jdbcTemplate.queryForList(
            "SELECT id, plan_id, user_id, name, category, quantity, packed, gear_pack_id FROM items WHERE plan_id = ? ORDER BY name",
            planId
        )
    }

    fun truncateAll() {
        // gear_packs and gear_pack_items are migration-seeded reference data — not truncated
        jdbcTemplate.execute("TRUNCATE TABLE items, plan_members, plans, users CASCADE")
    }
}
