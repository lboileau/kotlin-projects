package com.acme.services.camperservice.features.mealplan.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.mealplan.dto.*
import com.acme.services.camperservice.features.mealplan.params.*
import com.acme.services.camperservice.features.mealplan.service.MealPlanService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/meal-plans")
class MealPlanController(
    private val mealPlanService: MealPlanService
) {
    private val logger = LoggerFactory.getLogger(MealPlanController::class.java)

    /** POST /api/meal-plans — Create meal plan */
    @PostMapping
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CreateMealPlanRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/meal-plans")
        val param = CreateMealPlanParam(
            userId = userId,
            name = request.name,
            servings = request.servings,
            scalingMode = request.scalingMode,
            isTemplate = request.isTemplate,
            planId = request.planId,
        )
        return mealPlanService.create(param).toResponseEntity(successStatus = 201) { it }
    }

    /** GET /api/meal-plans/{id} — Get meal plan with full details */
    @GetMapping("/{id}")
    fun getDetail(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/meal-plans/{}", id)
        val param = GetMealPlanDetailParam(mealPlanId = id, userId = userId)
        return mealPlanService.getDetail(param).toResponseEntity { it }
    }

    /** GET /api/meal-plans?planId={planId} — Get meal plan for a trip */
    @GetMapping(params = ["planId"])
    fun getByPlanId(
        @RequestParam planId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/meal-plans?planId={}", planId)
        val param = GetMealPlanByPlanIdParam(planId = planId, userId = userId)
        return when (val result = mealPlanService.getByPlanId(param)) {
            is Result.Success -> if (result.value != null) {
                ResponseEntity.ok(result.value)
            } else {
                ResponseEntity.ok(null)
            }
            is Result.Failure -> result.error.toResponseEntity()
        }
    }

    /** GET /api/meal-plans/templates — List template meal plans */
    @GetMapping("/templates")
    fun getTemplates(
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/meal-plans/templates")
        val param = GetTemplatesParam(userId = userId)
        return mealPlanService.getTemplates(param).toResponseEntity { it }
    }

    /** PUT /api/meal-plans/{id} — Update meal plan settings */
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdateMealPlanRequest
    ): ResponseEntity<Any> {
        logger.info("PUT /api/meal-plans/{}", id)
        val param = UpdateMealPlanParam(
            mealPlanId = id,
            userId = userId,
            name = request.name,
            servings = request.servings,
            scalingMode = request.scalingMode,
        )
        return mealPlanService.update(param).toResponseEntity { it }
    }

    /** DELETE /api/meal-plans/{id} — Delete meal plan */
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/meal-plans/{}", id)
        val param = DeleteMealPlanParam(mealPlanId = id, userId = userId)
        return mealPlanService.delete(param).toResponseEntity(successStatus = 204) { }
    }

    /** POST /api/meal-plans/{id}/copy-to-trip — Copy template to a trip */
    @PostMapping("/{id}/copy-to-trip")
    fun copyToTrip(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: CopyToTripRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/meal-plans/{}/copy-to-trip", id)
        val param = CopyToTripParam(
            mealPlanId = id,
            userId = userId,
            planId = request.planId,
            servings = request.servings,
        )
        return mealPlanService.copyToTrip(param).toResponseEntity(successStatus = 201) { it }
    }

    /** POST /api/meal-plans/{id}/save-as-template — Save trip meal plan as template */
    @PostMapping("/{id}/save-as-template")
    fun saveAsTemplate(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: SaveAsTemplateRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/meal-plans/{}/save-as-template", id)
        val param = SaveAsTemplateParam(
            mealPlanId = id,
            userId = userId,
            name = request.name,
        )
        return mealPlanService.saveAsTemplate(param).toResponseEntity(successStatus = 201) { it }
    }

    /** POST /api/meal-plans/{id}/days — Add a day */
    @PostMapping("/{id}/days")
    fun addDay(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AddDayRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/meal-plans/{}/days", id)
        val param = AddDayParam(
            mealPlanId = id,
            userId = userId,
            dayNumber = request.dayNumber,
        )
        return mealPlanService.addDay(param).toResponseEntity(successStatus = 201) { it }
    }

    /** DELETE /api/meal-plans/{mealPlanId}/days/{dayId} — Remove a day */
    @DeleteMapping("/{mealPlanId}/days/{dayId}")
    fun removeDay(
        @PathVariable mealPlanId: UUID,
        @PathVariable dayId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/meal-plans/{}/days/{}", mealPlanId, dayId)
        val param = RemoveDayParam(mealPlanId = mealPlanId, dayId = dayId, userId = userId)
        return mealPlanService.removeDay(param).toResponseEntity(successStatus = 204) { }
    }

    /** POST /api/meal-plans/{mealPlanId}/days/{dayId}/recipes — Add recipe to a meal on a day */
    @PostMapping("/{mealPlanId}/days/{dayId}/recipes")
    fun addRecipeToMeal(
        @PathVariable mealPlanId: UUID,
        @PathVariable dayId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AddRecipeRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/meal-plans/{}/days/{}/recipes", mealPlanId, dayId)
        val param = AddRecipeToMealParam(
            mealPlanId = mealPlanId,
            dayId = dayId,
            userId = userId,
            mealType = request.mealType,
            recipeId = request.recipeId,
        )
        return mealPlanService.addRecipeToMeal(param).toResponseEntity(successStatus = 201) { it }
    }

    /** GET /api/meal-plans/{id}/shopping-list — Get computed shopping list */
    @GetMapping("/{id}/shopping-list")
    fun getShoppingList(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/meal-plans/{}/shopping-list", id)
        val param = GetShoppingListParam(mealPlanId = id, userId = userId)
        return mealPlanService.getShoppingList(param).toResponseEntity { it }
    }

    /** PATCH /api/meal-plans/{id}/shopping-list — Update purchased quantity */
    @PatchMapping("/{id}/shopping-list")
    fun updatePurchase(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: UpdatePurchaseRequest
    ): ResponseEntity<Any> {
        logger.info("PATCH /api/meal-plans/{}/shopping-list", id)
        val param = UpdatePurchaseParam(
            mealPlanId = id,
            userId = userId,
            ingredientId = request.ingredientId,
            unit = request.unit,
            quantityPurchased = request.quantityPurchased,
        )
        return mealPlanService.updatePurchase(param).toResponseEntity { it }
    }

    /** DELETE /api/meal-plans/{id}/shopping-list — Reset all purchases */
    @DeleteMapping("/{id}/shopping-list")
    fun resetPurchases(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/meal-plans/{}/shopping-list", id)
        val param = ResetPurchasesParam(mealPlanId = id, userId = userId)
        return mealPlanService.resetPurchases(param).toResponseEntity(successStatus = 204) { }
    }
}

/** Separate controller for meal-plan-recipe deletion (different base path) */
@RestController
class MealPlanRecipeController(
    private val mealPlanService: MealPlanService
) {
    private val logger = LoggerFactory.getLogger(MealPlanRecipeController::class.java)

    /** DELETE /api/meal-plan-recipes/{mealPlanRecipeId} — Remove recipe from meal */
    @DeleteMapping("/api/meal-plan-recipes/{mealPlanRecipeId}")
    fun removeRecipeFromMeal(
        @PathVariable mealPlanRecipeId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/meal-plan-recipes/{}", mealPlanRecipeId)
        val param = RemoveRecipeFromMealParam(mealPlanRecipeId = mealPlanRecipeId, userId = userId)
        return mealPlanService.removeRecipeFromMeal(param).toResponseEntity(successStatus = 204) { }
    }
}
