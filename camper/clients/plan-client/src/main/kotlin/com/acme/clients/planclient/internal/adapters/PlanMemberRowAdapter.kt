package com.acme.clients.planclient.internal.adapters

import com.acme.clients.planclient.model.PlanMember
import java.sql.ResultSet
import java.util.UUID

object PlanMemberRowAdapter {

    fun fromResultSet(rs: ResultSet): PlanMember = PlanMember(
        planId = rs.getObject("plan_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}
