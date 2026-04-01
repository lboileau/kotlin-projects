package com.acme.clients.itemclient.internal.adapters

import com.acme.clients.itemclient.model.Item
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [Item] domain objects.
 * Handles any drift between DB schema and data class over time.
 */
object ItemRowAdapter {

    fun fromResultSet(rs: ResultSet): Item = Item(
        id = rs.getObject("id", UUID::class.java),
        planId = rs.getObject("plan_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        name = rs.getString("name"),
        category = rs.getString("category"),
        quantity = rs.getInt("quantity"),
        packed = rs.getBoolean("packed"),
        gearPackId = rs.getObject("gear_pack_id", UUID::class.java),
        gearPackName = rs.getString("gear_pack_name"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
