package com.acme.clients.gearpackclient.internal.adapters

import com.acme.clients.gearpackclient.model.GearPackItem
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [GearPackItem] domain objects.
 */
object GearPackItemRowAdapter {

    fun fromResultSet(rs: ResultSet): GearPackItem = GearPackItem(
        id = rs.getObject("id", UUID::class.java),
        gearPackId = rs.getObject("gear_pack_id", UUID::class.java),
        name = rs.getString("name"),
        category = rs.getString("category"),
        defaultQuantity = rs.getInt("default_quantity"),
        scalable = rs.getBoolean("scalable"),
        sortOrder = rs.getInt("sort_order"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
