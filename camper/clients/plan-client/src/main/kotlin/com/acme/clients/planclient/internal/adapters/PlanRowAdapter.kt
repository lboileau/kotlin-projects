package com.acme.clients.planclient.internal.adapters

import com.acme.clients.planclient.model.Plan
import java.sql.ResultSet
import java.util.UUID

object PlanRowAdapter {

    fun fromResultSet(rs: ResultSet): Plan = Plan(
        id = rs.getObject("id", UUID::class.java),
        name = rs.getString("name"),
        visibility = rs.getString("visibility"),
        ownerId = rs.getObject("owner_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
