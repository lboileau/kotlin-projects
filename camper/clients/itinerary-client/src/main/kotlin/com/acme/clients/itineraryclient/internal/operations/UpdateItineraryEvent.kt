package com.acme.clients.itineraryclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.UpdateEventParam
import com.acme.clients.itineraryclient.internal.adapters.ItineraryEventRowAdapter
import com.acme.clients.itineraryclient.internal.validations.ValidateUpdateEvent
import com.acme.clients.itineraryclient.model.ItineraryEvent
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateItineraryEvent(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(UpdateItineraryEvent::class.java)
    private val validate = ValidateUpdateEvent()

    fun execute(param: UpdateEventParam): Result<ItineraryEvent, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Updating itinerary event id={}", param.id)
        val now = Instant.now()
        val rowsAffected = jdbi.withHandle<Int, Exception> { handle ->
            handle.createUpdate(
                """
                UPDATE itinerary_events
                SET title = :title, description = :description, details = :details, event_at = :eventAt,
                    category = :category, estimated_cost = :estimatedCost, location = :location, event_end_at = :eventEndAt,
                    updated_at = :updatedAt
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.id)
                .bind("title", param.title)
                .bind("description", param.description)
                .bind("details", param.details)
                .bind("eventAt", param.eventAt)
                .bind("category", param.category)
                .bind("estimatedCost", param.estimatedCost)
                .bind("location", param.location)
                .bind("eventEndAt", param.eventEndAt)
                .bind("updatedAt", now)
                .execute()
        }
        if (rowsAffected == 0) return failure(NotFoundError("ItineraryEvent", param.id.toString()))

        val entity = jdbi.withHandle<ItineraryEvent?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, itinerary_id, title, description, details, event_at, category, estimated_cost, location, event_end_at, created_at, updated_at
                FROM itinerary_events
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", param.id)
                .map { rs, _ -> ItineraryEventRowAdapter.fromResultSet(rs) }
                .findOne()
                .orElse(null)
        }
        return if (entity != null) success(entity) else failure(NotFoundError("ItineraryEvent", param.id.toString()))
    }
}
