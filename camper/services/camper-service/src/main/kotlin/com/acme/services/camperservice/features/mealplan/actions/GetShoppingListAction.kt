package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.services.camperservice.features.mealplan.dto.ShoppingListResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.GetShoppingListParam

internal class GetShoppingListAction {
    fun execute(param: GetShoppingListParam): Result<ShoppingListResponse, MealPlanError> {
        TODO("Implementation in service-impl PR")
    }
}
