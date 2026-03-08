package com.acme.clients.itineraryclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.*
import com.acme.clients.itineraryclient.internal.validations.ValidateAddEvent
import com.acme.clients.itineraryclient.internal.validations.ValidateCreateItinerary
import com.acme.clients.itineraryclient.internal.validations.ValidateUpdateEvent
import com.acme.clients.itineraryclient.model.Itinerary
import com.acme.clients.itineraryclient.model.ItineraryEvent
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeItineraryClient : ItineraryClient {
    private val itineraryStore = ConcurrentHashMap<UUID, Itinerary>()
    private val eventStore = ConcurrentHashMap<UUID, ItineraryEvent>()

    private val validateCreateItinerary = ValidateCreateItinerary()
    private val validateAddEvent = ValidateAddEvent()
    private val validateUpdateEvent = ValidateUpdateEvent()

    override fun getByPlanId(param: GetByPlanIdParam): Result<Itinerary, AppError> {
        val entity = itineraryStore.values.find { it.planId == param.planId }
        return if (entity != null) success(entity) else failure(NotFoundError("Itinerary", param.planId.toString()))
    }

    override fun create(param: CreateItineraryParam): Result<Itinerary, AppError> {
        val validation = validateCreateItinerary.execute(param)
        if (validation is Result.Failure) return validation

        if (itineraryStore.values.any { it.planId == param.planId }) {
            return failure(ConflictError("Itinerary", "plan '${param.planId}' already has an itinerary"))
        }
        val entity = Itinerary(
            id = UUID.randomUUID(),
            planId = param.planId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        itineraryStore[entity.id] = entity
        return success(entity)
    }

    override fun delete(param: DeleteItineraryParam): Result<Unit, AppError> {
        val entity = itineraryStore.values.find { it.planId == param.planId }
            ?: return failure(NotFoundError("Itinerary", param.planId.toString()))
        itineraryStore.remove(entity.id)
        eventStore.entries.removeAll { it.value.itineraryId == entity.id }
        return success(Unit)
    }

    override fun getEvents(param: GetEventsParam): Result<List<ItineraryEvent>, AppError> {
        val events = eventStore.values
            .filter { it.itineraryId == param.itineraryId }
            .sortedBy { it.eventAt }
        return success(events)
    }

    override fun addEvent(param: AddEventParam): Result<ItineraryEvent, AppError> {
        val validation = validateAddEvent.execute(param)
        if (validation is Result.Failure) return validation

        val entity = ItineraryEvent(
            id = UUID.randomUUID(),
            itineraryId = param.itineraryId,
            title = param.title,
            description = param.description,
            details = param.details,
            eventAt = param.eventAt,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        eventStore[entity.id] = entity
        return success(entity)
    }

    override fun updateEvent(param: UpdateEventParam): Result<ItineraryEvent, AppError> {
        val validation = validateUpdateEvent.execute(param)
        if (validation is Result.Failure) return validation

        val existing = eventStore[param.id]
            ?: return failure(NotFoundError("ItineraryEvent", param.id.toString()))
        val updated = existing.copy(
            title = param.title,
            description = param.description,
            details = param.details,
            eventAt = param.eventAt,
            updatedAt = Instant.now()
        )
        eventStore[param.id] = updated
        return success(updated)
    }

    override fun deleteEvent(param: DeleteEventParam): Result<Unit, AppError> {
        return if (eventStore.remove(param.id) != null) success(Unit) else failure(NotFoundError("ItineraryEvent", param.id.toString()))
    }

    fun reset() {
        itineraryStore.clear()
        eventStore.clear()
    }

    fun seedItinerary(vararg entities: Itinerary) {
        entities.forEach { itineraryStore[it.id] = it }
    }

    fun seedEvent(vararg entities: ItineraryEvent) {
        entities.forEach { eventStore[it.id] = it }
    }
}
