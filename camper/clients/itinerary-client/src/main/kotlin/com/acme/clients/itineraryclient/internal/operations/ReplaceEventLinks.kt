package com.acme.clients.itineraryclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.ReplaceEventLinksParam
import com.acme.clients.itineraryclient.internal.validations.ValidateReplaceEventLinks
import com.acme.clients.itineraryclient.model.ItineraryEventLink
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class ReplaceEventLinks(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(ReplaceEventLinks::class.java)
    private val validate = ValidateReplaceEventLinks()

    fun execute(param: ReplaceEventLinksParam): Result<List<ItineraryEventLink>, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Replacing links for eventId={}", param.eventId)
        val entities = jdbi.inTransaction<List<ItineraryEventLink>, Exception> { handle ->
            handle.createUpdate("DELETE FROM itinerary_event_links WHERE event_id = :eventId")
                .bind("eventId", param.eventId)
                .execute()

            val now = Instant.now()
            param.links.map { link ->
                val id = UUID.randomUUID()
                handle.createUpdate(
                    """
                    INSERT INTO itinerary_event_links (id, event_id, url, label, created_at)
                    VALUES (:id, :eventId, :url, :label, :createdAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("eventId", param.eventId)
                    .bind("url", link.url)
                    .bind("label", link.label)
                    .bind("createdAt", now)
                    .execute()
                ItineraryEventLink(
                    id = id,
                    eventId = param.eventId,
                    url = link.url,
                    label = link.label,
                    createdAt = now
                )
            }
        }
        return success(entities)
    }
}
