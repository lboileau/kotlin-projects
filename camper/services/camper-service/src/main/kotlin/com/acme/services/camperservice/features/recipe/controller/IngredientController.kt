package com.acme.services.camperservice.features.recipe.controller

import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.recipe.dto.CreateIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.UpdateIngredientRequest
import com.acme.services.camperservice.features.recipe.params.CreateIngredientParam
import com.acme.services.camperservice.features.recipe.params.DeleteIngredientParam
import com.acme.services.camperservice.features.recipe.params.ListIngredientsParam
import com.acme.services.camperservice.features.recipe.params.UpdateIngredientParam
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
        return ingredientService.list(ListIngredientsParam(userId)).toResponseEntity { it }
    }

    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateIngredientRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/ingredients")
        val param = CreateIngredientParam(
            userId = userId,
            name = request.name,
            category = request.category,
            defaultUnit = request.defaultUnit
        )
        return ingredientService.create(param).toResponseEntity(successStatus = 201) { it }
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateIngredientRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/ingredients/{}", id)
        val param = UpdateIngredientParam(
            ingredientId = id,
            userId = userId,
            name = request.name,
            category = request.category,
            defaultUnit = request.defaultUnit
        )
        return ingredientService.update(param).toResponseEntity { it }
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/ingredients/{}", id)
        return ingredientService.delete(DeleteIngredientParam(ingredientId = id, userId = userId))
            .toResponseEntity(successStatus = 204) { }
    }
}
