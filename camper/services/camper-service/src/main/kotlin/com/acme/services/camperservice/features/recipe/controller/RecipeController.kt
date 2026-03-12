package com.acme.services.camperservice.features.recipe.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.recipe.dto.CreateRecipeIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.CreateRecipeRequest
import com.acme.services.camperservice.features.recipe.dto.ImportRecipeRequest
import com.acme.services.camperservice.features.recipe.dto.ResolveDuplicateRequest
import com.acme.services.camperservice.features.recipe.dto.ResolveIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.UpdateRecipeRequest
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.*
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
        val param = CreateRecipeParam(
            userId = userId,
            name = request.name,
            description = request.description,
            webLink = request.webLink,
            baseServings = request.baseServings,
            meal = request.meal,
            theme = request.theme,
            ingredients = request.ingredients.map {
                CreateRecipeIngredientParam(
                    ingredientId = it.ingredientId,
                    quantity = it.quantity,
                    unit = it.unit
                )
            }
        )
        return recipeService.create(param).toResponseEntity(successStatus = 201) { it }
    }

    @PostMapping("/import")
    fun import(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ImportRecipeRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/recipes/import")
        return recipeService.import(ImportRecipeParam(userId = userId, url = request.url))
            .toResponseEntity(successStatus = 201) { it }
    }

    @GetMapping
    fun list(
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/recipes")
        return recipeService.list(ListRecipesParam(userId)).toResponseEntity { it }
    }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/recipes/{}", id)
        return recipeService.get(GetRecipeParam(recipeId = id, userId = userId)).toResponseEntity { it }
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateRecipeRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/recipes/{}", id)
        val param = UpdateRecipeParam(
            recipeId = id,
            userId = userId,
            name = request.name,
            description = request.description,
            baseServings = request.baseServings,
            meal = request.meal,
            theme = request.theme
        )
        return recipeService.update(param).toResponseEntity { it }
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/recipes/{}", id)
        return recipeService.delete(DeleteRecipeParam(recipeId = id, userId = userId))
            .toResponseEntity(successStatus = 204) { }
    }

    @PutMapping("/{id}/ingredients/{ingredientId}")
    fun resolveIngredient(
        @PathVariable id: UUID,
        @PathVariable ingredientId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ResolveIngredientRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/recipes/{}/ingredients/{}", id, ingredientId)
        val param = ResolveIngredientParam(
            recipeId = id,
            recipeIngredientId = ingredientId,
            userId = userId,
            action = request.action,
            ingredientId = request.ingredientId,
            newIngredient = request.newIngredient,
            quantity = request.quantity,
            unit = request.unit
        )
        return recipeService.resolveIngredient(param).toResponseEntity { it }
    }

    @PutMapping("/{id}/resolve-duplicate")
    fun resolveDuplicate(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: ResolveDuplicateRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/recipes/{}/resolve-duplicate", id)
        val param = ResolveDuplicateParam(recipeId = id, userId = userId, action = request.action)
        return when (val result = recipeService.resolveDuplicate(param)) {
            is Result.Success -> if (result.value != null) {
                ResponseEntity.ok(result.value)
            } else {
                ResponseEntity.noContent().build()
            }
            is Result.Failure -> result.error.toResponseEntity()
        }
    }

    @PostMapping("/{id}/ingredients")
    fun addIngredient(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateRecipeIngredientRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/recipes/{}/ingredients", id)
        val param = AddRecipeIngredientParam(
            recipeId = id,
            userId = userId,
            ingredientId = request.ingredientId,
            quantity = request.quantity,
            unit = request.unit
        )
        return recipeService.addIngredient(param).toResponseEntity(successStatus = 201) { it }
    }

    @DeleteMapping("/{id}/ingredients/{ingredientId}")
    fun removeIngredient(
        @PathVariable id: UUID,
        @PathVariable ingredientId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/recipes/{}/ingredients/{}", id, ingredientId)
        val param = RemoveRecipeIngredientParam(recipeId = id, recipeIngredientId = ingredientId, userId = userId)
        return recipeService.removeIngredient(param).toResponseEntity(successStatus = 204) { }
    }

    @PostMapping("/{id}/publish")
    fun publish(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("POST /api/recipes/{}/publish", id)
        return recipeService.publish(PublishRecipeParam(recipeId = id, userId = userId))
            .toResponseEntity { it }
    }
}
