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
import com.acme.clients.itineraryclient.internal.validations.ValidateReplaceEventLinks
import com.acme.clients.itineraryclient.internal.validations.ValidateUpdateEvent
import com.acme.clients.itineraryclient.model.Itinerary
import com.acme.clients.itineraryclient.model.ItineraryEvent
import com.acme.clients.itineraryclient.model.ItineraryEventLink
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeItineraryClient : ItineraryClient {
    private val itineraryStore = ConcurrentHashMap<UUID, Itinerary>()
    private val eventStore = ConcurrentHashMap<UUID, ItineraryEvent>()
    private val linkStore = ConcurrentHashMap<UUID, ItineraryEventLink>()

    private val validateCreateItinerary = ValidateCreateItinerary()
    private val validateAddEvent = ValidateAddEvent()
    private val validateUpdateEvent = ValidateUpdateEvent()
    private val validateReplaceEventLinks = ValidateReplaceEventLinks()

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
        val eventIds = eventStore.values.filter { it.itineraryId == entity.id }.map { it.id }.toSet()
        linkStore.entries.removeAll { it.value.eventId in eventIds }
        eventStore.entries.removeAll { it.value.itineraryId == entity.id }
        itineraryStore.remove(entity.id)
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
            category = param.category,
            estimatedCost = param.estimatedCost,
            location = param.location,
            eventEndAt = param.eventEndAt,
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
            category = param.category,
            estimatedCost = param.estimatedCost,
            location = param.location,
            eventEndAt = param.eventEndAt,
            updatedAt = Instant.now()
        )
        eventStore[param.id] = updated
        return success(updated)
    }

    override fun deleteEvent(param: DeleteEventParam): Result<Unit, AppError> {
        linkStore.entries.removeAll { it.value.eventId == param.id }
        return if (eventStore.remove(param.id) != null) success(Unit) else failure(NotFoundError("ItineraryEvent", param.id.toString()))
    }

    override fun getLinksByEventIds(param: GetLinksByEventIdsParam): Result<List<ItineraryEventLink>, AppError> {
        if (param.eventIds.isEmpty()) return success(emptyList())
        val eventIds = param.eventIds.toSet()
        val links = linkStore.values
            .filter { it.eventId in eventIds }
            .sortedBy { it.createdAt }
        return success(links)
    }

    override fun replaceEventLinks(param: ReplaceEventLinksParam): Result<List<ItineraryEventLink>, AppError> {
        val validation = validateReplaceEventLinks.execute(param)
        if (validation is Result.Failure) return validation

        linkStore.entries.removeAll { it.value.eventId == param.eventId }
        val now = Instant.now()
        val newLinks = param.links.map { link ->
            ItineraryEventLink(
                id = UUID.randomUUID(),
                eventId = param.eventId,
                url = link.url,
                label = link.label,
                createdAt = now
            )
        }
        newLinks.forEach { linkStore[it.id] = it }
        return success(newLinks)
    }

    fun reset() {
        itineraryStore.clear()
        eventStore.clear()
        linkStore.clear()
    }

    fun seedItinerary(vararg entities: Itinerary) {
        entities.forEach { itineraryStore[it.id] = it }
    }

    fun seedEvent(vararg entities: ItineraryEvent) {
        entities.forEach { eventStore[it.id] = it }
    }

    fun seedLink(vararg entities: ItineraryEventLink) {
        entities.forEach { linkStore[it.id] = it }
    }
}
