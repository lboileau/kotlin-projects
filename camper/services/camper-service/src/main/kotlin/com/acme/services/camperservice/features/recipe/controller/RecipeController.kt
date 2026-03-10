package com.acme.services.camperservice.features.recipe.controller

import com.acme.services.camperservice.features.recipe.dto.CreateRecipeRequest
import com.acme.services.camperservice.features.recipe.dto.ImportRecipeRequest
import com.acme.services.camperservice.features.recipe.dto.ResolveDuplicateRequest
import com.acme.services.camperservice.features.recipe.dto.ResolveIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.UpdateRecipeRequest
import com.acme.services.camperservice.features.recipe.service.RecipeService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/recipes")
class RecipeController(
    private val recipeService: RecipeService
) {
    private val logger = LoggerFactory.getLogger(RecipeController::class.java)

    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateRecipeRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/recipes")
        return ResponseEntity.status(501).build()
    }

    @PostMapping("/import")
    fun import(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ImportRecipeRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/recipes/import")
        return ResponseEntity.status(501).build()
    }

    @GetMapping
    fun list(
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/recipes")
        return ResponseEntity.status(501).build()
    }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/recipes/{}", id)
        return ResponseEntity.status(501).build()
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateRecipeRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/recipes/{}", id)
        return ResponseEntity.status(501).build()
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/recipes/{}", id)
        return ResponseEntity.status(501).build()
    }

    @PutMapping("/{id}/ingredients/{ingredientId}")
    fun resolveIngredient(
        @PathVariable id: UUID,
        @PathVariable ingredientId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ResolveIngredientRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/recipes/{}/ingredients/{}", id, ingredientId)
        return ResponseEntity.status(501).build()
    }

    @PutMapping("/{id}/resolve-duplicate")
    fun resolveDuplicate(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ResolveDuplicateRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/recipes/{}/resolve-duplicate", id)
        return ResponseEntity.status(501).build()
    }

    @PostMapping("/{id}/publish")
    fun publish(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("POST /api/recipes/{}/publish", id)
        return ResponseEntity.status(501).build()
    }
}
