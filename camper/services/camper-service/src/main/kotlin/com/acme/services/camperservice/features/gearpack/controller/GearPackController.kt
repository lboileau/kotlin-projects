package com.acme.services.camperservice.features.gearpack.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.gearpack.dto.AddGearPackItemRequest
import com.acme.services.camperservice.features.gearpack.dto.ApplyGearPackRequest
import com.acme.services.camperservice.features.gearpack.dto.CreateGearPackRequest
import com.acme.services.camperservice.features.gearpack.dto.UpdateGearPackItemRequest
import com.acme.services.camperservice.features.gearpack.dto.UpdateGearPackRequest
import com.acme.services.camperservice.features.gearpack.mapper.GearPackMapper
import com.acme.services.camperservice.features.gearpack.params.AddGearPackItemParam
import com.acme.services.camperservice.features.gearpack.params.ApplyGearPackParam
import com.acme.services.camperservice.features.gearpack.params.CreateGearPackParam
import com.acme.services.camperservice.features.gearpack.params.DeleteGearPackParam
import com.acme.services.camperservice.features.gearpack.params.GetGearPackParam
import com.acme.services.camperservice.features.gearpack.params.ListGearPacksParam
import com.acme.services.camperservice.features.gearpack.params.RemoveGearPackItemParam
import com.acme.services.camperservice.features.gearpack.params.SearchGearPackItemsParam
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackItemParam
import com.acme.services.camperservice.features.gearpack.params.UpdateGearPackParam
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

    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateGearPackRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/gear-packs")
        val param = CreateGearPackParam(
            name = request.name,
            description = request.description,
            requestingUserId = userId,
        )
        return gearPackService.create(param).toResponseEntity(successStatus = 201) { GearPackMapper.toDetailResponse(it) }
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateGearPackRequest,
    ): ResponseEntity<Any> {
        logger.info("PUT /api/gear-packs/{}", id)
        val param = UpdateGearPackParam(
            id = id,
            name = request.name,
            description = request.description,
            requestingUserId = userId,
        )
        return gearPackService.update(param).toResponseEntity { GearPackMapper.toDetailResponse(it) }
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/gear-packs/{}", id)
        val param = DeleteGearPackParam(id = id, requestingUserId = userId)
        return gearPackService.delete(param).toResponseEntity(successStatus = 204) { }
    }

    @PostMapping("/{id}/items")
    fun addItem(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AddGearPackItemRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/gear-packs/{}/items", id)
        val param = AddGearPackItemParam(
            gearPackId = id,
            name = request.name,
            category = request.category,
            defaultQuantity = request.defaultQuantity,
            scalable = request.scalable,
            requestingUserId = userId,
        )
        return gearPackService.addItem(param).toResponseEntity(successStatus = 201) { it }
    }

    @PutMapping("/{id}/items/{itemId}")
    fun updateItem(
        @PathVariable id: UUID,
        @PathVariable itemId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateGearPackItemRequest,
    ): ResponseEntity<Any> {
        logger.info("PUT /api/gear-packs/{}/items/{}", id, itemId)
        val param = UpdateGearPackItemParam(
            id = itemId,
            gearPackId = id,
            name = request.name,
            category = request.category,
            defaultQuantity = request.defaultQuantity,
            scalable = request.scalable,
            requestingUserId = userId,
        )
        return gearPackService.updateItem(param).toResponseEntity { it }
    }

    @DeleteMapping("/{id}/items/{itemId}")
    fun removeItem(
        @PathVariable id: UUID,
        @PathVariable itemId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/gear-packs/{}/items/{}", id, itemId)
        val param = RemoveGearPackItemParam(id = itemId, gearPackId = id, requestingUserId = userId)
        return gearPackService.removeItem(param).toResponseEntity(successStatus = 204) { }
    }
}

@RestController
@RequestMapping("/api/gear-pack-items")
class GearPackItemsController(
    private val gearPackService: GearPackService,
) {
    private val logger = LoggerFactory.getLogger(GearPackItemsController::class.java)

    @GetMapping("/search")
    fun searchItems(
        @RequestParam q: String,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("GET /api/gear-pack-items/search?q={}", q)
        val param = SearchGearPackItemsParam(query = q, requestingUserId = userId)
        return gearPackService.searchItems(param).toResponseEntity { it }
    }
}
