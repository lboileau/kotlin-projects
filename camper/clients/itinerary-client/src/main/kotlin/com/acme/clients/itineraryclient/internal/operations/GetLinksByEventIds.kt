package com.acme.clients.itineraryclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.GetLinksByEventIdsParam
import com.acme.clients.itineraryclient.internal.adapters.ItineraryEventLinkRowAdapter
import com.acme.clients.itineraryclient.model.ItineraryEventLink
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetLinksByEventIds(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetLinksByEventIds::class.java)

    fun execute(param: GetLinksByEventIdsParam): Result<List<ItineraryEventLink>, AppError> {
        if (param.eventIds.isEmpty()) return success(emptyList())

        logger.debug("Finding links for eventIds={}", param.eventIds)
        val entities = jdbi.withHandle<List<ItineraryEventLink>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, event_id, url, label, created_at
                FROM itinerary_event_links
                WHERE event_id IN (<eventIds>)
                ORDER BY created_at ASC
                """.trimIndent()
            )
                .bindList("eventIds", param.eventIds)
                .map { rs, _ -> ItineraryEventLinkRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
