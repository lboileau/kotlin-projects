package com.acme.services.camperservice.features.gearpack.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.gearpack.dto.ApplyGearPackRequest
import com.acme.services.camperservice.features.gearpack.mapper.GearPackMapper
import com.acme.services.camperservice.features.gearpack.params.ApplyGearPackParam
import com.acme.services.camperservice.features.gearpack.params.GetGearPackParam
import com.acme.services.camperservice.features.gearpack.params.ListGearPacksParam
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
        val param = ListGearPacksParam(requestingUserId = userId)
        return gearPackService.list(param).toResponseEntity { packs ->
            packs.map { GearPackMapper.toSummaryResponse(it) }
        }
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("GET /api/gear-packs/{}", id)
        val param = GetGearPackParam(id = id, requestingUserId = userId)
        return gearPackService.getById(param).toResponseEntity { GearPackMapper.toDetailResponse(it) }
    }

    @PostMapping("/{id}/apply")
    fun apply(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ApplyGearPackRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/gear-packs/{}/apply", id)
        val param = ApplyGearPackParam(
            gearPackId = id,
            planId = request.planId,
            groupSize = request.groupSize,
            requestingUserId = userId,
        )
        val result = gearPackService.apply(param)
        if (result is Result.Success) {
            eventPublisher.publishUpdate(request.planId, "items", "updated")
        }
        return result.toResponseEntity(successStatus = 201) { GearPackMapper.toApplyResponse(it) }
    }
}
