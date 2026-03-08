package com.acme.services.camperservice.features.item.controller

import com.acme.services.camperservice.features.item.dto.CreateItemRequest
import com.acme.services.camperservice.features.item.dto.UpdateItemRequest
import com.acme.services.camperservice.features.item.params.*
import com.acme.services.camperservice.features.item.service.ItemService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/items")
class ItemController(private val itemService: ItemService) {
    private val logger = LoggerFactory.getLogger(ItemController::class.java)

    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateItemRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/items")
        return ResponseEntity.status(501).build()
    }

    @GetMapping
    fun getByOwner(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestParam ownerType: String,
        @RequestParam ownerId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/items?ownerType={}&ownerId={}", ownerType, ownerId)
        return ResponseEntity.status(501).build()
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/items/{}", id)
        return ResponseEntity.status(501).build()
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateItemRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/items/{}", id)
        return ResponseEntity.status(501).build()
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/items/{}", id)
        return ResponseEntity.status(501).build()
    }
}
