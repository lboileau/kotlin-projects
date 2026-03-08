package com.acme.services.camperservice.features.itinerary.controller

import com.acme.services.camperservice.features.itinerary.dto.AddEventRequest
import com.acme.services.camperservice.features.itinerary.dto.UpdateEventRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/plans/{planId}/itinerary")
class ItineraryController {
    private val logger = LoggerFactory.getLogger(ItineraryController::class.java)

    @GetMapping
    fun getItinerary(@PathVariable planId: UUID): ResponseEntity<Any> {
        logger.info("GET /api/plans/{}/itinerary", planId)
        return ResponseEntity.status(501).body("Not Implemented")
    }

    @DeleteMapping
    fun deleteItinerary(@PathVariable planId: UUID): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/itinerary", planId)
        return ResponseEntity.status(501).body("Not Implemented")
    }

    @PostMapping("/events")
    fun addEvent(
        @PathVariable planId: UUID,
        @RequestBody request: AddEventRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/plans/{}/itinerary/events", planId)
        return ResponseEntity.status(501).body("Not Implemented")
    }

    @PutMapping("/events/{eventId}")
    fun updateEvent(
        @PathVariable planId: UUID,
        @PathVariable eventId: UUID,
        @RequestBody request: UpdateEventRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/plans/{}/itinerary/events/{}", planId, eventId)
        return ResponseEntity.status(501).body("Not Implemented")
    }

    @DeleteMapping("/events/{eventId}")
    fun deleteEvent(
        @PathVariable planId: UUID,
        @PathVariable eventId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/plans/{}/itinerary/events/{}", planId, eventId)
        return ResponseEntity.status(501).body("Not Implemented")
    }
}
