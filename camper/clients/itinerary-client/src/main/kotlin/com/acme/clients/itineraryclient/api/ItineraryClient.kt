package com.acme.clients.itineraryclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.itineraryclient.model.Itinerary
import com.acme.clients.itineraryclient.model.ItineraryEvent
import com.acme.clients.itineraryclient.model.ItineraryEventLink

/**
 * Client interface for Itinerary entity operations.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface ItineraryClient {
    /** Retrieve an itinerary by its associated plan ID. */
    fun getByPlanId(param: GetByPlanIdParam): Result<Itinerary, AppError>

    /** Create a new itinerary for a plan. */
    fun create(param: CreateItineraryParam): Result<Itinerary, AppError>

    /** Delete an itinerary by its associated plan ID. */
    fun delete(param: DeleteItineraryParam): Result<Unit, AppError>

    /** Retrieve all events for an itinerary. */
    fun getEvents(param: GetEventsParam): Result<List<ItineraryEvent>, AppError>

    /** Add a new event to an itinerary. */
    fun addEvent(param: AddEventParam): Result<ItineraryEvent, AppError>

    /** Update an existing itinerary event. */
    fun updateEvent(param: UpdateEventParam): Result<ItineraryEvent, AppError>

    /** Delete an itinerary event. */
    fun deleteEvent(param: DeleteEventParam): Result<Unit, AppError>

    /** Retrieve all links for a list of events. */
    fun getLinksByEventIds(param: GetLinksByEventIdsParam): Result<List<ItineraryEventLink>, AppError>

    /** Replace all links for an event (delete existing, insert new). */
    fun replaceEventLinks(param: ReplaceEventLinksParam): Result<List<ItineraryEventLink>, AppError>
}
