package com.acme.clients.logbookclient.internal.adapters

import com.acme.clients.logbookclient.model.LogBookJournalEntry
import java.sql.ResultSet
import java.util.UUID

/**
 * Adapts database rows to [LogBookJournalEntry] domain objects.
 */
object LogBookJournalEntryRowAdapter {

    fun fromResultSet(rs: ResultSet): LogBookJournalEntry = LogBookJournalEntry(
        id = rs.getObject("id", UUID::class.java),
        planId = rs.getObject("plan_id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        pageNumber = rs.getInt("page_number"),
        content = rs.getString("content"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
    )
}
