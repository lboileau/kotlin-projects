package com.acme.clients.itineraryclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.CreateItineraryParam
import com.acme.clients.itineraryclient.internal.validations.ValidateCreateItinerary
import com.acme.clients.itineraryclient.model.Itinerary
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

internal class CreateItinerary(private val jdbi: Jdbi) {
    private val logger = LoggerFactory.getLogger(CreateItinerary::class.java)
    private val validate = ValidateCreateItinerary()

    fun execute(param: CreateItineraryParam): Result<Itinerary, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        logger.debug("Creating itinerary for planId={}", param.planId)
        return try {
            val entity = jdbi.withHandle<Itinerary, Exception> { handle ->
                val id = UUID.randomUUID()
                val now = Instant.now()
                handle.createUpdate(
                    """
                    INSERT INTO itineraries (id, plan_id, created_at, updated_at)
                    VALUES (:id, :planId, :createdAt, :updatedAt)
                    """.trimIndent()
                )
                    .bind("id", id)
                    .bind("planId", param.planId)
                    .bind("createdAt", now)
                    .bind("updatedAt", now)
                    .execute()
                Itinerary(id = id, planId = param.planId, createdAt = now, updatedAt = now)
            }
            success(entity)
        } catch (e: Exception) {
            if (e.message?.contains("uq_itineraries_plan_id") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("Itinerary", "plan '${param.planId}' already has an itinerary"))
            } else {
                throw e
            }
        }
    }
}
