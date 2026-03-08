package com.acme.services.camperservice.features.itinerary.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.itinerary.dto.AddEventRequest
import com.acme.services.camperservice.features.itinerary.dto.UpdateEventRequest
import com.acme.services.camperservice.features.itinerary.mapper.ItineraryMapper
import com.acme.services.camperservice.features.itinerary.params.*
import com.acme.services.camperservice.features.itinerary.service.ItineraryService
import com.acme.services.camperservice.websocket.PlanEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/plans/{planId}/itinerary")
class ItineraryController(
    private val itineraryService: ItineraryService,
    private val eventPublisher: PlanEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(ItineraryController::class.java)

    @GetMapping
    fun getItinerary(@PathVariable planId: UUID): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/itinerary", planId)
        val param = GetItineraryParam(planId = planId)
        return itineraryService.getItinerary(param).toResponseEntity { (itinerary, events) ->
            ItineraryMapper.toResponse(itinerary, events)
        }
    }

    @DeleteMapping
    fun deleteItinerary(@PathVariable planId: UUID): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/itinerary", planId)
        val param = DeleteItineraryParam(planId = planId)
        val result = itineraryService.deleteItinerary(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "itinerary", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }

    @PostMapping("/events")
    fun addEvent(
        @PathVariable planId: UUID,
        @RequestBody request: AddEventRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/itinerary/events", planId)
        val param = AddEventParam(
            planId = planId,
            title = request.title,
            description = request.description,
            details = request.details,
            eventAt = request.eventAt
        )
        val result = itineraryService.addEvent(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "itinerary", "updated")
        return result.toResponseEntity(successStatus = 201) {
            ItineraryMapper.toResponse(it)
        }
    }

    @PutMapping("/events/{eventId}")
    fun updateEvent(
        @PathVariable planId: UUID,
        @PathVariable eventId: UUID,
        @RequestBody request: UpdateEventRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/itinerary/events/{}", planId, eventId)
        val param = UpdateEventParam(
            planId = planId,
            eventId = eventId,
            title = request.title,
            description = request.description,
            details = request.details,
            eventAt = request.eventAt
        )
        val result = itineraryService.updateEvent(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "itinerary", "updated")
        return result.toResponseEntity { ItineraryMapper.toResponse(it) }
    }

    @DeleteMapping("/events/{eventId}")
    fun deleteEvent(
        @PathVariable planId: UUID,
        @PathVariable eventId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/itinerary/events/{}", planId, eventId)
        val param = DeleteEventParam(planId = planId, eventId = eventId)
        val result = itineraryService.deleteEvent(param)
        if (result is Result.Success) eventPublisher.publishUpdate(planId, "itinerary", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }
}
