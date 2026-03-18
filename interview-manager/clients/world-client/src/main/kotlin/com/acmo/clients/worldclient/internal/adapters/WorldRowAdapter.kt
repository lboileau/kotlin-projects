package com.acmo.clients.worldclient.internal.adapters

import com.acmo.clients.worldclient.model.World
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [World] domain objects.
 * Handles any drift between DB schema and data class over time.
 */
object WorldRowAdapter {

    fun fromResultSet(rs: ResultSet): World = World(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        greeting = rs.getString("greeting"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
