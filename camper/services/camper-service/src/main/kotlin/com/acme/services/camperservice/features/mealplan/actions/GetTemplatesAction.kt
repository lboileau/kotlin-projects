package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.services.camperservice.features.mealplan.dto.MealPlanResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.GetTemplatesParam

internal class GetTemplatesAction {
    fun execute(param: GetTemplatesParam): Result<List<MealPlanResponse>, MealPlanError> {
        TODO("Implementation in service-impl PR")
    }
}
