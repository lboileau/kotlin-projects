package com.acme.clients.gearpackclient.internal.adapters

import com.acme.clients.gearpackclient.model.GearPack
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [GearPack] domain objects.
 * The [items] field is always set to an empty list — callers populate it separately.
 */
object GearPackRowAdapter {

    fun fromResultSet(rs: ResultSet): GearPack = GearPack(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        description = rs.getString("description"),
        items = emptyList(),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
