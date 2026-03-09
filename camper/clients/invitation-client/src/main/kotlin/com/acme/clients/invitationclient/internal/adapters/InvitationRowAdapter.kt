package com.acme.clients.invitationclient.internal.adapters

import com.acme.clients.invitationclient.model.Invitation
import java.sql.ResultSet
import java.util.UUID

internal object InvitationRowAdapter {
    fun fromResultSet(rs: ResultSet): Invitation = Invitation(
        id = rs.getObject("id", UUID::class.java),
        planId = rs.getObject("plan_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        email = rs.getString("email"),
        inviterId = rs.getObject("inviter_id", UUID::class.java),
        resendEmailId = rs.getString("resend_email_id"),
        status = rs.getString("status"),
        sentAt = rs.getTimestamp("sent_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
