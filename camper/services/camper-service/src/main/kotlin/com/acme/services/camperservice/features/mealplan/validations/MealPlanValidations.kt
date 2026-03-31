package com.acme.services.camperservice.features.mealplan.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.*

private val VALID_SCALING_MODES = setOf("fractional", "round_up")
private val VALID_MEAL_TYPES = setOf("breakfast", "lunch", "dinner", "snack")

internal class ValidateCreateMealPlan {
    fun execute(param: CreateMealPlanParam): Result<Unit, MealPlanError> {
        if (param.name.isBlank()) return Result.Failure(MealPlanError.Invalid("name", "must not be blank"))
        if (param.name.length > 255) return Result.Failure(MealPlanError.Invalid("name", "must not exceed 255 characters"))
        if (param.servings <= 0) return Result.Failure(MealPlanError.Invalid("servings", "must be greater than 0"))
        if (param.scalingMode != null && param.scalingMode !in VALID_SCALING_MODES) {
            return Result.Failure(MealPlanError.Invalid("scalingMode", "must be 'fractional' or 'round_up'"))
        }
        return success(Unit)
    }
}

internal class ValidateGetMealPlanDetail {
    fun execute(param: GetMealPlanDetailParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateGetMealPlanByPlanId {
    fun execute(param: GetMealPlanByPlanIdParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateGetTemplates {
    fun execute(param: GetTemplatesParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateUpdateMealPlan {
    fun execute(param: UpdateMealPlanParam): Result<Unit, MealPlanError> {
        if (param.name != null && param.name.isBlank()) return Result.Failure(MealPlanError.Invalid("name", "must not be blank"))
        if (param.name != null && param.name.length > 255) return Result.Failure(MealPlanError.Invalid("name", "must not exceed 255 characters"))
        if (param.servings != null && param.servings <= 0) return Result.Failure(MealPlanError.Invalid("servings", "must be greater than 0"))
        if (param.scalingMode != null && param.scalingMode !in VALID_SCALING_MODES) {
            return Result.Failure(MealPlanError.Invalid("scalingMode", "must be 'fractional' or 'round_up'"))
        }
        return success(Unit)
    }
}

internal class ValidateDeleteMealPlan {
    fun execute(param: DeleteMealPlanParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateCopyToTrip {
    fun execute(param: CopyToTripParam): Result<Unit, MealPlanError> {
        if (param.servings != null && param.servings <= 0) {
            return Result.Failure(MealPlanError.Invalid("servings", "must be greater than 0"))
        }
        return success(Unit)
    }
}

internal class ValidateSaveAsTemplate {
    fun execute(param: SaveAsTemplateParam): Result<Unit, MealPlanError> {
        if (param.name.isBlank()) return Result.Failure(MealPlanError.Invalid("name", "must not be blank"))
        if (param.name.length > 255) return Result.Failure(MealPlanError.Invalid("name", "must not exceed 255 characters"))
        return success(Unit)
    }
}

internal class ValidateAddDay {
    fun execute(param: AddDayParam): Result<Unit, MealPlanError> {
        if (param.dayNumber <= 0) return Result.Failure(MealPlanError.Invalid("dayNumber", "must be greater than 0"))
        return success(Unit)
    }
}

internal class ValidateRemoveDay {
    fun execute(param: RemoveDayParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateAddRecipeToMeal {
    fun execute(param: AddRecipeToMealParam): Result<Unit, MealPlanError> {
        if (param.mealType !in VALID_MEAL_TYPES) {
            return Result.Failure(MealPlanError.Invalid("mealType", "must be 'breakfast', 'lunch', 'dinner', or 'snack'"))
        }
        return success(Unit)
    }
}

internal class ValidateRemoveRecipeFromMeal {
    fun execute(param: RemoveRecipeFromMealParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateGetShoppingList {
    fun execute(param: GetShoppingListParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateUpdatePurchase {
    fun execute(param: UpdatePurchaseParam): Result<Unit, MealPlanError> {
        if (param.ingredientId == null && param.manualItemId == null) {
            return Result.Failure(MealPlanError.Invalid("ingredientId/manualItemId", "exactly one must be provided"))
        }
        if (param.ingredientId != null && param.manualItemId != null) {
            return Result.Failure(MealPlanError.Invalid("ingredientId/manualItemId", "exactly one must be provided"))
        }
        if (param.ingredientId != null && param.unit == null) {
            return Result.Failure(MealPlanError.Invalid("unit", "must be provided when ingredientId is used"))
        }
        if (param.quantityPurchased < java.math.BigDecimal.ZERO) {
            return Result.Failure(MealPlanError.Invalid("quantityPurchased", "must be >= 0"))
        }
        return success(Unit)
    }
}

internal class ValidateAddManualItem {
    fun execute(param: AddManualItemParam): Result<Unit, MealPlanError> {
        if (param.ingredientId == null && param.description == null) {
            return Result.Failure(MealPlanError.Invalid("ingredientId/description", "either ingredientId or description must be provided"))
        }
        if (param.ingredientId != null && param.description != null) {
            return Result.Failure(MealPlanError.Invalid("ingredientId/description", "only one of ingredientId or description may be provided"))
        }
        if (param.ingredientId != null) {
            if (param.quantity == null || param.quantity <= java.math.BigDecimal.ZERO) {
                return Result.Failure(MealPlanError.Invalid("quantity", "must be greater than 0 for ingredient-based items"))
            }
            if (param.unit == null) {
                return Result.Failure(MealPlanError.Invalid("unit", "must be provided for ingredient-based items"))
            }
        }
        if (param.description != null) {
            if (param.quantity != null) {
                return Result.Failure(MealPlanError.Invalid("quantity", "must not be provided for free-form items"))
            }
            if (param.unit != null) {
                return Result.Failure(MealPlanError.Invalid("unit", "must not be provided for free-form items"))
            }
            if (param.description.isBlank()) {
                return Result.Failure(MealPlanError.Invalid("description", "must not be blank"))
            }
            if (param.description.length > 500) {
                return Result.Failure(MealPlanError.Invalid("description", "must not exceed 500 characters"))
            }
        }
        return success(Unit)
    }
}

internal class ValidateRemoveManualItem {
    fun execute(param: RemoveManualItemParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateResetPurchases {
    fun execute(param: ResetPurchasesParam): Result<Unit, MealPlanError> = success(Unit)
}
