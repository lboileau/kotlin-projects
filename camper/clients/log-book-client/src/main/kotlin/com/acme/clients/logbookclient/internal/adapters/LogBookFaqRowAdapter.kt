package com.acme.clients.logbookclient.internal.adapters

import com.acme.clients.logbookclient.model.LogBookFaq
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [LogBookFaq] domain objects.
 */
object LogBookFaqRowAdapter {

    fun fromResultSet(rs: ResultSet): LogBookFaq = LogBookFaq(
        id = rs.getObject("id", UUID::class.java),
        planId = rs.getObject("plan_id", UUID::class.java),
        question = rs.getString("question"),
        askedById = rs.getObject("asked_by_id", UUID::class.java),
        answer = rs.getString("answer"),
        answeredById = rs.getObject("answered_by_id", UUID::class.java),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
