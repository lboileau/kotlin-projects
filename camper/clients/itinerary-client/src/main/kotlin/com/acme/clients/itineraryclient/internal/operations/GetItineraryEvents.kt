package com.acme.clients.itineraryclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.GetEventsParam
import com.acme.clients.itineraryclient.internal.adapters.ItineraryEventRowAdapter
import com.acme.clients.itineraryclient.model.ItineraryEvent
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

internal class GetItineraryEvents(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(GetItineraryEvents::class.java)

    fun execute(param: GetEventsParam): Result<List<ItineraryEvent>, AppError> {
        logger.debug("Finding events for itineraryId={}", param.itineraryId)
        val entities = jdbi.withHandle<List<ItineraryEvent>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, itinerary_id, title, description, details, event_at, created_at, updated_at
                FROM itinerary_events
                WHERE itinerary_id = :itineraryId
                ORDER BY event_at ASC
                """.trimIndent()
            )
                .bind("itineraryId", param.itineraryId)
                .map { rs, _ -> ItineraryEventRowAdapter.fromResultSet(rs) }
                .list()
        }
        return success(entities)
    }
}
