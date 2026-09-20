package com.acme.services.camperservice.features.mealplan.controller

import com.acme.clients.common.Result
import com.acme.services.camperservice.common.error.toResponseEntity
import com.acme.services.camperservice.features.mealplan.dto.*
import com.acme.services.camperservice.features.mealplan.params.*
import com.acme.services.camperservice.features.mealplan.service.MealPlanService
import com.acme.services.camperservice.websocket.MealPlanEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/meal-plans")
class MealPlanController(
    private val mealPlanService: MealPlanService,
    private val eventPublisher: MealPlanEventPublisher,
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

    /** GET /api/meal-plans?createdBy={createdBy} — List meal plans created by a user (403 unless createdBy is the caller) */
    @GetMapping(params = ["createdBy"])
    fun listByCreatedBy(
        @RequestParam createdBy: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/meal-plans?createdBy={}", createdBy)
        val param = ListMealPlansByCreatorParam(createdBy = createdBy, userId = userId)
        return mealPlanService.listMealPlansByCreator(param).toResponseEntity { it }
    }

    /** GET /api/meal-plans/mine — Meal plans the caller owns plus meal plans shared with them */
    @GetMapping("/mine")
    fun getMine(
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/meal-plans/mine")
        val param = GetMineParam(userId = userId)
        return mealPlanService.getMine(param).toResponseEntity { it }
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
        val result = mealPlanService.update(param)
        if (result is Result.Success) eventPublisher.publishUpdate(id, "meal-plan", "updated")
        return result.toResponseEntity { it }
    }

    /** DELETE /api/meal-plans/{id} — Delete meal plan */
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/meal-plans/{}", id)
        val param = DeleteMealPlanParam(mealPlanId = id, userId = userId)
        val result = mealPlanService.delete(param)
        if (result is Result.Success) eventPublisher.publishUpdate(id, "meal-plan", "deleted")
        return result.toResponseEntity(successStatus = 204) { }
    }

    /** POST /api/meal-plans/{mealPlanId}/duplicate — Duplicate a meal plan (flat copy; no sync event) */
    @PostMapping("/{mealPlanId}/duplicate")
    fun duplicate(
        @PathVariable mealPlanId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: DuplicateMealPlanRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/meal-plans/{}/duplicate", mealPlanId)
        val param = DuplicateMealPlanParam(mealPlanId = mealPlanId, userId = userId, name = request.name)
        return mealPlanService.duplicate(param).toResponseEntity(successStatus = 201) { it }
    }

    /** POST /api/meal-plans/{mealPlanId}/recipes — Plan-level add recipe (flat plans): lowest-numbered day, mealType dinner, idempotent */
    @PostMapping("/{mealPlanId}/recipes")
    fun addRecipeToPlan(
        @PathVariable mealPlanId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AddRecipeToPlanRequest
    ): ResponseEntity<Any> {
        logger.info("POST /api/meal-plans/{}/recipes", mealPlanId)
        val param = AddRecipeToPlanParam(mealPlanId = mealPlanId, userId = userId, recipeId = request.recipeId)
        return when (val result = mealPlanService.addRecipeToPlan(param)) {
            is Result.Success -> {
                val (detail, created) = result.value
                if (created) eventPublisher.publishUpdate(mealPlanId, "meal-plan", "updated")
                ResponseEntity.status(if (created) 201 else 200).body(detail)
            }
            is Result.Failure -> result.error.toResponseEntity()
        }
    }

    /** DELETE /api/meal-plans/{mealPlanId}/recipes/{recipeId} — Plan-level remove recipe (flat plans): removes every occurrence, idempotent */
    @DeleteMapping("/{mealPlanId}/recipes/{recipeId}")
    fun removeRecipeFromPlan(
        @PathVariable mealPlanId: UUID,
        @PathVariable recipeId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/meal-plans/{}/recipes/{}", mealPlanId, recipeId)
        val param = RemoveRecipeFromPlanParam(mealPlanId = mealPlanId, userId = userId, recipeId = recipeId)
        val result = mealPlanService.removeRecipeFromPlan(param)
        if (result is Result.Success && result.value > 0) eventPublisher.publishUpdate(mealPlanId, "meal-plan", "updated")
        return result.toResponseEntity(successStatus = 204) { }
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
        val result = mealPlanService.addDay(param)
        if (result is Result.Success) eventPublisher.publishUpdate(id, "meal-plan", "updated")
        return result.toResponseEntity(successStatus = 201) { it }
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
        val result = mealPlanService.removeDay(param)
        if (result is Result.Success) eventPublisher.publishUpdate(mealPlanId, "meal-plan", "updated")
        return result.toResponseEntity(successStatus = 204) { }
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
        val result = mealPlanService.addRecipeToMeal(param)
        if (result is Result.Success) eventPublisher.publishUpdate(mealPlanId, "meal-plan", "updated")
        return result.toResponseEntity(successStatus = 201) { it }
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
            manualItemId = request.manualItemId,
            unit = request.unit,
            quantityPurchased = request.quantityPurchased,
        )
        val result = mealPlanService.updatePurchase(param)
        if (result is Result.Success) eventPublisher.publishUpdate(id, "shopping-list", "updated")
        return result.toResponseEntity { it }
    }

    /** POST /api/meal-plans/{id}/shopping-list/items — Add manual item */
    @PostMapping("/{id}/shopping-list/items")
    fun addManualItem(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: AddManualItemRequest,
    ): ResponseEntity<Any> {
        logger.info("POST /api/meal-plans/{}/shopping-list/items", id)
        val param = AddManualItemParam(
            mealPlanId = id,
            userId = userId,
            ingredientId = request.ingredientId,
            description = request.description,
            quantity = request.quantity,
            unit = request.unit,
        )
        val result = mealPlanService.addManualItem(param)
        if (result is Result.Success) eventPublisher.publishUpdate(id, "shopping-list", "updated")
        return result.toResponseEntity(successStatus = 201) { it }
    }

    /** DELETE /api/meal-plans/{id}/shopping-list/items/{itemId} — Remove manual item */
    @DeleteMapping("/{id}/shopping-list/items/{itemId}")
    fun removeManualItem(
        @PathVariable id: UUID,
        @PathVariable itemId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/meal-plans/{}/shopping-list/items/{}", id, itemId)
        val param = RemoveManualItemParam(
            mealPlanId = id,
            userId = userId,
            itemId = itemId,
        )
        val result = mealPlanService.removeManualItem(param)
        if (result is Result.Success) eventPublisher.publishUpdate(id, "shopping-list", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }

    /** DELETE /api/meal-plans/{id}/shopping-list — Reset all purchases */
    @DeleteMapping("/{id}/shopping-list")
    fun resetPurchases(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/meal-plans/{}/shopping-list", id)
        val param = ResetPurchasesParam(mealPlanId = id, userId = userId)
        val result = mealPlanService.resetPurchases(param)
        if (result is Result.Success) eventPublisher.publishUpdate(id, "shopping-list", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }

    /** GET /api/meal-plans/{id}/share — Get (lazily creating) the meal plan's share token */
    @GetMapping("/{id}/share")
    fun getShareToken(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/meal-plans/{}/share", id)
        val param = GetShareTokenParam(mealPlanId = id, userId = userId)
        return mealPlanService.getShareToken(param).toResponseEntity { it }
    }

    /** GET /api/meal-plans/{id}/members — Owner or member: list members, owner first */
    @GetMapping("/{id}/members")
    fun getMembers(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("GET /api/meal-plans/{}/members", id)
        val param = GetMealPlanMembersParam(mealPlanId = id, userId = userId)
        return mealPlanService.getMembers(param).toResponseEntity { it }
    }

    /** DELETE /api/meal-plans/{id}/members/{userId} — Owner removes anyone, a member removes themselves (leave) */
    @DeleteMapping("/{id}/members/{userId}")
    fun removeMember(
        @PathVariable id: UUID,
        @PathVariable userId: UUID,
        @RequestHeader("X-User-Id") requestingUserId: UUID
    ): ResponseEntity<Any> {
        logger.info("DELETE /api/meal-plans/{}/members/{}", id, userId)
        val param = RemoveMealPlanMemberParam(mealPlanId = id, targetUserId = userId, userId = requestingUserId)
        val result = mealPlanService.removeMember(param)
        if (result is Result.Success && result.value) eventPublisher.publishUpdate(id, "members", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }
}

/** Separate controller for meal-plan-recipe deletion (different base path) */
@RestController
class MealPlanRecipeController(
    private val mealPlanService: MealPlanService,
    private val eventPublisher: MealPlanEventPublisher,
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
        val result = mealPlanService.removeRecipeFromMeal(param)
        if (result is Result.Success) eventPublisher.publishUpdate(result.value, "meal-plan", "updated")
        return result.toResponseEntity(successStatus = 204) { }
    }
}

/** Separate controller for accepting a meal plan share link (different base path) */
@RestController
class MealPlanInviteController(
    private val mealPlanService: MealPlanService,
    private val eventPublisher: MealPlanEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(MealPlanInviteController::class.java)

    /** POST /api/meal-plan-invites/{token}/accept — Join a meal plan via its share link, idempotent */
    @PostMapping("/api/meal-plan-invites/{token}/accept")
    fun accept(
        @PathVariable token: String,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Any> {
        logger.info("POST /api/meal-plan-invites/{}/accept", token)
        val param = AcceptInviteParam(token = token, userId = userId)
        val result = mealPlanService.acceptInvite(param)
        if (result is Result.Success && !result.value.alreadyMember) {
            eventPublisher.publishUpdate(result.value.mealPlanId, "members", "updated")
        }
        return result.toResponseEntity { it }
    }
}
