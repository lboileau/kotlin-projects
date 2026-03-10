package com.acme.services.camperservice.features.recipe.controller

import com.acme.services.camperservice.features.recipe.dto.CreateIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.UpdateIngredientRequest
import com.acme.services.camperservice.features.recipe.service.IngredientService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/ingredients")
class IngredientController(
    private val ingredientService: IngredientService
) {
    private val logger = LoggerFactory.getLogger(IngredientController::class.java)

    @GetMapping
    fun list(
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/ingredients")
        return ResponseEntity.status(501).build()
    }

    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateIngredientRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/ingredients")
        return ResponseEntity.status(501).build()
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateIngredientRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/ingredients/{}", id)
        return ResponseEntity.status(501).build()
    }
}
