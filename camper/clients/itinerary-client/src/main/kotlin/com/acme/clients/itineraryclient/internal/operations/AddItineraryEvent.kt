package com.acme.clients.itineraryclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.AddEventParam
import com.acme.clients.itineraryclient.internal.validations.ValidateAddEvent
import com.acme.clients.itineraryclient.model.ItineraryEvent
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class AddItineraryEvent(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(AddItineraryEvent::class.java)
    private val validate = ValidateAddEvent()

    fun execute(param: AddEventParam): Result<ItineraryEvent, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Adding event to itineraryId={}", param.itineraryId)
        val entity = jdbi.withHandle<ItineraryEvent, Exception> { handle ->
            val id = UUID.randomUUID()
            val now = Instant.now()
            handle.createUpdate(
                """
                INSERT INTO itinerary_events (id, itinerary_id, title, description, details, event_at, category, estimated_cost, location, event_end_at, created_at, updated_at)
                VALUES (:id, :itineraryId, :title, :description, :details, :eventAt, :category, :estimatedCost, :location, :eventEndAt, :createdAt, :updatedAt)
                """.trimIndent()
            )
                .bind("id", id)
                .bind("itineraryId", param.itineraryId)
                .bind("title", param.title)
                .bind("description", param.description)
                .bind("details", param.details)
                .bind("eventAt", param.eventAt)
                .bind("category", param.category)
                .bind("estimatedCost", param.estimatedCost)
                .bind("location", param.location)
                .bind("eventEndAt", param.eventEndAt)
                .bind("createdAt", now)
                .bind("updatedAt", now)
                .execute()
            ItineraryEvent(
                id = id,
                itineraryId = param.itineraryId,
                title = param.title,
                description = param.description,
                details = param.details,
                eventAt = param.eventAt,
                category = param.category,
                estimatedCost = param.estimatedCost,
                location = param.location,
                eventEndAt = param.eventEndAt,
                createdAt = now,
                updatedAt = now
            )
        }
        return success(entity)
    }
}
