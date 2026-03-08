package com.acme.services.camperservice.features.itinerary.service

import com.acme.clients.itineraryclient.api.ItineraryClient
import com.acme.clients.planclient.api.PlanClient
import com.acme.services.camperservice.features.itinerary.actions.*
import com.acme.services.camperservice.features.itinerary.params.*

class ItineraryService(itineraryClient: ItineraryClient, planClient: PlanClient) {
    private val getItinerary = GetItineraryAction(itineraryClient, planClient)
    private val deleteItinerary = DeleteItineraryAction(itineraryClient, planClient)
    private val addEvent = AddEventAction(itineraryClient, planClient)
    private val updateEvent = UpdateEventAction(itineraryClient)
    private val deleteEvent = DeleteEventAction(itineraryClient)

    fun getItinerary(param: GetItineraryParam) = getItinerary.execute(param)
    fun deleteItinerary(param: DeleteItineraryParam) = deleteItinerary.execute(param)
    fun addEvent(param: AddEventParam) = addEvent.execute(param)
    fun updateEvent(param: UpdateEventParam) = updateEvent.execute(param)
    fun deleteEvent(param: DeleteEventParam) = deleteEvent.execute(param)
}
