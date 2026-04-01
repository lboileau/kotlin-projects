package com.acme.clients.itineraryclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.itineraryclient.api.*
import com.acme.clients.itineraryclient.internal.operations.*
import com.acme.clients.itineraryclient.model.Itinerary
import com.acme.clients.itineraryclient.model.ItineraryEvent
import com.acme.clients.itineraryclient.model.ItineraryEventLink
import org.jdbi.v3.core.Jdbi

/**
 * Facade that delegates to individual operation classes.
 */
internal class JdbiItineraryClient(jdbi: Jdbi) : ItineraryClient {

    private val getItineraryByPlanId = GetItineraryByPlanId(jdbi)
    private val createItinerary = CreateItinerary(jdbi)
    private val deleteItinerary = DeleteItinerary(jdbi)
    private val getItineraryEvents = GetItineraryEvents(jdbi)
    private val addItineraryEvent = AddItineraryEvent(jdbi)
    private val updateItineraryEvent = UpdateItineraryEvent(jdbi)
    private val deleteItineraryEvent = DeleteItineraryEvent(jdbi)

    override fun getByPlanId(param: GetByPlanIdParam): Result<Itinerary, AppError> = getItineraryByPlanId.execute(param)
    override fun create(param: CreateItineraryParam): Result<Itinerary, AppError> = createItinerary.execute(param)
    override fun delete(param: DeleteItineraryParam): Result<Unit, AppError> = deleteItinerary.execute(param)
    override fun getEvents(param: GetEventsParam): Result<List<ItineraryEvent>, AppError> = getItineraryEvents.execute(param)
    override fun addEvent(param: AddEventParam): Result<ItineraryEvent, AppError> = addItineraryEvent.execute(param)
    override fun updateEvent(param: UpdateEventParam): Result<ItineraryEvent, AppError> = updateItineraryEvent.execute(param)
    override fun deleteEvent(param: DeleteEventParam): Result<Unit, AppError> = deleteItineraryEvent.execute(param)
    override fun getLinksByEventIds(param: GetLinksByEventIdsParam): Result<List<ItineraryEventLink>, AppError> =
        throw NotImplementedError("getLinksByEventIds is not yet implemented")
    override fun replaceEventLinks(param: ReplaceEventLinksParam): Result<List<ItineraryEventLink>, AppError> =
        throw NotImplementedError("replaceEventLinks is not yet implemented")
}
