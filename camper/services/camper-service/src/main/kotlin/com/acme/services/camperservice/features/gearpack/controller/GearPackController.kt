package com.acme.services.camperservice.features.gearpack.controller

import com.acme.services.camperservice.features.gearpack.dto.ApplyGearPackRequest
import com.acme.services.camperservice.features.gearpack.service.GearPackService
import com.acme.services.camperservice.websocket.PlanEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/gear-packs")
class GearPackController(
    private val gearPackService: GearPackService,
    private val eventPublisher: PlanEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(GearPackController::class.java)

    @GetMapping
    fun list(@RequestHeader("X-User-Id") userId: UUID): ResponseEntity<Any> {
        logger.info("GET /api/gear-packs")
        return ResponseEntity.status(501).body("Not implemented")
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("GET /api/gear-packs/{}", id)
        return ResponseEntity.status(501).body("Not implemented")
    }

    @PostMapping("/{id}/apply")
    fun apply(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ApplyGearPackRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/gear-packs/{}/apply", id)
        return ResponseEntity.status(501).body("Not implemented")
    }
}
