package com.acme.clients.assignmentclient.internal.adapters

import com.acme.clients.assignmentclient.model.AssignmentMember
import java.sql.ResultSet
import java.util.UUID

object AssignmentMemberRowAdapter {

    fun fromResultSet(rs: ResultSet): AssignmentMember = AssignmentMember(
        assignmentId = rs.getObject("assignment_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        planId = rs.getObject("plan_id", UUID::class.java),
        assignmentType = rs.getString("assignment_type"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
