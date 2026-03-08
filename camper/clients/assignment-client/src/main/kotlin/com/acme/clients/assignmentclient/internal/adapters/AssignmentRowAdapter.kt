package com.acme.clients.assignmentclient.internal.adapters

import com.acme.clients.assignmentclient.model.Assignment
import java.sql.ResultSet
import java.util.UUID

object AssignmentRowAdapter {

    fun fromResultSet(rs: ResultSet): Assignment = Assignment(
        id = rs.getObject("id", UUID::class.java),
        planId = rs.getObject("plan_id", UUID::class.java),
        name = rs.getString("name"),
        type = rs.getString("type"),
        maxOccupancy = rs.getInt("max_occupancy"),
        ownerId = rs.getObject("owner_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
