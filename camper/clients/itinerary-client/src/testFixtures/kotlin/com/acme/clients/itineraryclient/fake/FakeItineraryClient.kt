package com.acme.clients.itineraryclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.itineraryclient.api.*
import com.acme.clients.itineraryclient.model.Itinerary
import com.acme.clients.itineraryclient.model.ItineraryEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeItineraryClient : ItineraryClient {
    private val itineraryStore = ConcurrentHashMap<UUID, Itinerary>()
    private val eventStore = ConcurrentHashMap<UUID, ItineraryEvent>()

    override fun getByPlanId(param: GetByPlanIdParam): Result<Itinerary, AppError> {
        throw NotImplementedError()
    }

    override fun create(param: CreateItineraryParam): Result<Itinerary, AppError> {
        throw NotImplementedError()
    }

    override fun delete(param: DeleteItineraryParam): Result<Unit, AppError> {
        throw NotImplementedError()
    }

    override fun getEvents(param: GetEventsParam): Result<List<ItineraryEvent>, AppError> {
        throw NotImplementedError()
    }

    override fun addEvent(param: AddEventParam): Result<ItineraryEvent, AppError> {
        throw NotImplementedError()
    }

    override fun updateEvent(param: UpdateEventParam): Result<ItineraryEvent, AppError> {
        throw NotImplementedError()
    }

    override fun deleteEvent(param: DeleteEventParam): Result<Unit, AppError> {
        throw NotImplementedError()
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
