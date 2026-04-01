package com.acme.services.camperservice.features.item.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.item.dto.CreateItemRequest
import com.acme.services.camperservice.features.item.dto.UpdateItemRequest
import com.acme.services.camperservice.features.item.mapper.ItemMapper
import com.acme.services.camperservice.features.item.params.*
import com.acme.services.camperservice.features.item.service.ItemService
import com.acme.services.camperservice.websocket.PlanEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/items")
class ItemController(
    private val itemService: ItemService,
    private val eventPublisher: PlanEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(ItemController::class.java)

    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateItemRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/items")
        val param = CreateItemParam(
            name = request.name,
            category = request.category,
            quantity = request.quantity,
            packed = request.packed,
            ownerType = request.ownerType,
            ownerId = request.ownerId,
            planId = request.planId,
            gearPackId = request.gearPackId,
            requestingUserId = userId,
        )
        val result = itemService.create(param)
        if (result is Result.Success) {
            val planId = if (request.ownerType == "plan") request.ownerId else result.value.planId
            planId?.let { eventPublisher.publishUpdate(it, "items", "updated") }
        }
        return result.toResponseEntity(successStatus = 201) { ItemMapper.toResponse(it) }
    }

    @GetMapping
    fun getByOwner(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam ownerType: String,
        @RequestParam ownerId: UUID,
        @RequestParam(required = false) planId: UUID?
    ): ResponseEntity<Any> {
        logger.info("GET /api/items?ownerType={}&ownerId={}&planId={}", ownerType, ownerId, planId)
        val param = GetItemsByOwnerParam(ownerType = ownerType, ownerId = ownerId, planId = planId, requestingUserId = userId)
        return itemService.getByOwner(param).toResponseEntity { list -> list.map { ItemMapper.toResponse(it) } }
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/items/{}", id)
        val param = GetItemParam(id = id, requestingUserId = userId)
        return itemService.getById(param).toResponseEntity { ItemMapper.toResponse(it) }
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateItemRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/items/{}", id)
        val param = UpdateItemParam(
            id = id,
            name = request.name,
            category = request.category,
            quantity = request.quantity,
            packed = request.packed,
            gearPackId = request.gearPackId,
            requestingUserId = userId,
        )
        val result = itemService.update(param)
        if (result is Result.Success) {
            result.value.planId?.let { eventPublisher.publishUpdate(it, "items", "updated") }
        }
        return result.toResponseEntity { ItemMapper.toResponse(it) }
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/items/{}", id)
        val planId = (itemService.getById(GetItemParam(id = id, requestingUserId = userId)) as? Result.Success)?.value?.planId
        val param = DeleteItemParam(id = id, requestingUserId = userId)
        val result = itemService.delete(param)
        if (result is Result.Success) {
            planId?.let { eventPublisher.publishUpdate(it, "items", "updated") }
        }
        return result.toResponseEntity(successStatus = 204) { }
    }
}
