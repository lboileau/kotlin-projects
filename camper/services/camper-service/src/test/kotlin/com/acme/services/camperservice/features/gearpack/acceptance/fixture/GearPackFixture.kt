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
        // TRUNCATE users CASCADE now cascades to gear_packs via the V038 FK (gear_packs.created_by -> users.id)
        // Re-seed gear_packs and gear_pack_items after truncation since they are migration-managed reference data
        jdbcTemplate.execute("TRUNCATE TABLE items, plan_members, plans, users CASCADE")
        reseedGearPackReferenceData()
    }

    private fun reseedGearPackReferenceData() {
        jdbcTemplate.update(
            """INSERT INTO gear_packs (id, name, description)
               VALUES ('cc000000-0001-4000-8000-000000000001', 'Cooking Equipment', 'Essential cooking gear for campfire meals. Includes pots, pans, utensils, and tableware.')
               ON CONFLICT (id) DO NOTHING"""
        )
        jdbcTemplate.update(
            """INSERT INTO gear_pack_items (id, gear_pack_id, name, category, default_quantity, scalable, sort_order) VALUES
               ('cc000000-0001-4000-8000-000000000101', 'cc000000-0001-4000-8000-000000000001', 'Cast Iron Pan', 'kitchen', 1, false, 1),
               ('cc000000-0001-4000-8000-000000000102', 'cc000000-0001-4000-8000-000000000001', 'Grill Grate', 'kitchen', 1, false, 2),
               ('cc000000-0001-4000-8000-000000000103', 'cc000000-0001-4000-8000-000000000001', 'Large Pot', 'kitchen', 1, false, 3),
               ('cc000000-0001-4000-8000-000000000104', 'cc000000-0001-4000-8000-000000000001', 'Spatula', 'kitchen', 1, false, 4),
               ('cc000000-0001-4000-8000-000000000105', 'cc000000-0001-4000-8000-000000000001', 'Tongs', 'kitchen', 1, false, 5),
               ('cc000000-0001-4000-8000-000000000106', 'cc000000-0001-4000-8000-000000000001', 'Cooking Knife', 'kitchen', 1, false, 6),
               ('cc000000-0001-4000-8000-000000000107', 'cc000000-0001-4000-8000-000000000001', 'Cutting Board', 'kitchen', 1, false, 7),
               ('cc000000-0001-4000-8000-000000000108', 'cc000000-0001-4000-8000-000000000001', 'Can Opener', 'kitchen', 1, false, 8),
               ('cc000000-0001-4000-8000-000000000109', 'cc000000-0001-4000-8000-000000000001', 'Plates', 'kitchen', 1, true, 9),
               ('cc000000-0001-4000-8000-000000000110', 'cc000000-0001-4000-8000-000000000001', 'Cups', 'kitchen', 1, true, 10),
               ('cc000000-0001-4000-8000-000000000111', 'cc000000-0001-4000-8000-000000000001', 'Bowls', 'kitchen', 1, true, 11),
               ('cc000000-0001-4000-8000-000000000112', 'cc000000-0001-4000-8000-000000000001', 'Cutlery Set', 'kitchen', 1, true, 12)
               ON CONFLICT (id) DO NOTHING"""
        )
    }
}
