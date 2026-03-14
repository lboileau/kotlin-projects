package com.acme.services.camperservice.common.error

import com.acme.clients.common.Result
import com.acme.services.common.ApiResponse
import com.acme.services.camperservice.features.assignment.error.AssignmentError
import com.acme.services.camperservice.features.gearsync.error.GearSyncError
import com.acme.services.camperservice.features.item.error.ItemError
import com.acme.services.camperservice.features.itinerary.error.ItineraryError
import com.acme.services.camperservice.features.plan.error.PlanError
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.logbook.error.LogBookError
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.world.error.WorldError
import org.springframework.http.ResponseEntity

fun WorldError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is WorldError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is WorldError.AlreadyExists -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is WorldError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}

@JvmName("worldResultToResponseEntity")
fun <T> Result<T, WorldError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun UserError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is UserError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is UserError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is UserError.Forbidden -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
    is UserError.RegistrationRequired -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("REGISTRATION_REQUIRED", message))
}

@JvmName("userResultToResponseEntity")
fun <T> Result<T, UserError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun PlanError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is PlanError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is PlanError.NotOwner -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
    is PlanError.AlreadyMember -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is PlanError.NotMember -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is PlanError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is PlanError.CannotChangeOwnerRole -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}

@JvmName("planResultToResponseEntity")
fun <T> Result<T, PlanError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun ItemError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is ItemError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is ItemError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is ItemError.Forbidden -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
}

@JvmName("itemResultToResponseEntity")
fun <T> Result<T, ItemError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun ItineraryError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is ItineraryError.PlanNotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is ItineraryError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is ItineraryError.EventNotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is ItineraryError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}

@JvmName("itineraryResultToResponseEntity")
fun <T> Result<T, ItineraryError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun GearSyncError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is GearSyncError.PlanNotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
}

@JvmName("gearSyncResultToResponseEntity")
fun <T> Result<T, GearSyncError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun AssignmentError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is AssignmentError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is AssignmentError.NotOwner -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
    is AssignmentError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is AssignmentError.AtCapacity -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is AssignmentError.AlreadyAssigned -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is AssignmentError.AlreadyMember -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is AssignmentError.CannotRemoveOwner -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is AssignmentError.PlanNotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is AssignmentError.DuplicateName -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
}

@JvmName("assignmentResultToResponseEntity")
fun <T> Result<T, AssignmentError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun MealPlanError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is MealPlanError.MealPlanNotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is MealPlanError.DayNotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is MealPlanError.RecipeNotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is MealPlanError.DuplicateDayNumber -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is MealPlanError.PlanAlreadyHasMealPlan -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is MealPlanError.NotATemplate -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is MealPlanError.IsATemplate -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is MealPlanError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}

@JvmName("mealPlanResultToResponseEntity")
fun <T> Result<T, MealPlanError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun LogBookError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is LogBookError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is LogBookError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is LogBookError.Forbidden -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
}

@JvmName("logBookResultToResponseEntity")
fun <T> Result<T, LogBookError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}

fun RecipeError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is RecipeError.NotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is RecipeError.NotCreator -> ResponseEntity.status(403)
        .body(ApiResponse.ErrorBody("FORBIDDEN", message))
    is RecipeError.Invalid -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
    is RecipeError.DuplicateWebLink -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is RecipeError.DuplicateIngredientName -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
    is RecipeError.UnresolvedIngredients -> ResponseEntity.status(422)
        .body(ApiResponse.ErrorBody("UNPROCESSABLE_ENTITY", message))
    is RecipeError.UnresolvedDuplicate -> ResponseEntity.status(422)
        .body(ApiResponse.ErrorBody("UNPROCESSABLE_ENTITY", message))
    is RecipeError.ImportFailed -> ResponseEntity.status(422)
        .body(ApiResponse.ErrorBody("IMPORT_FAILED", message))
    is RecipeError.ScrapeFailed -> ResponseEntity.status(422)
        .body(ApiResponse.ErrorBody("SCRAPE_FAILED", message))
    is RecipeError.IngredientNotFound -> ResponseEntity.status(404)
        .body(ApiResponse.ErrorBody("NOT_FOUND", message))
    is RecipeError.AlreadyPublished -> ResponseEntity.status(409)
        .body(ApiResponse.ErrorBody("CONFLICT", message))
}

@JvmName("recipeResultToResponseEntity")
fun <T> Result<T, RecipeError>.toResponseEntity(
    successStatus: Int = 200,
    transform: (T) -> Any = { it as Any }
): ResponseEntity<Any> = when (this) {
    is Result.Success -> ResponseEntity.status(successStatus).body(transform(value))
    is Result.Failure -> error.toResponseEntity()
}
