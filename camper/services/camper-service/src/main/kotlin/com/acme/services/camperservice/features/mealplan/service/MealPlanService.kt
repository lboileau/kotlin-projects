package com.acme.services.camperservice.features.mealplan.service

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.mealplan.actions.AddDayAction
import com.acme.services.camperservice.features.mealplan.actions.AddManualItemAction
import com.acme.services.camperservice.features.mealplan.actions.AddRecipeToMealAction
import com.acme.services.camperservice.features.mealplan.actions.CopyToTripAction
import com.acme.services.camperservice.features.mealplan.actions.CreateMealPlanAction
import com.acme.services.camperservice.features.mealplan.actions.DeleteMealPlanAction
import com.acme.services.camperservice.features.mealplan.actions.GetMealPlanByPlanIdAction
import com.acme.services.camperservice.features.mealplan.actions.GetMealPlanDetailAction
import com.acme.services.camperservice.features.mealplan.actions.GetShoppingListAction
import com.acme.services.camperservice.features.mealplan.actions.GetTemplatesAction
import com.acme.services.camperservice.features.mealplan.actions.RemoveDayAction
import com.acme.services.camperservice.features.mealplan.actions.RemoveManualItemAction
import com.acme.services.camperservice.features.mealplan.actions.RemoveRecipeFromMealAction
import com.acme.services.camperservice.features.mealplan.actions.ResetPurchasesAction
import com.acme.services.camperservice.features.mealplan.actions.SaveAsTemplateAction
import com.acme.services.camperservice.features.mealplan.actions.UpdateMealPlanAction
import com.acme.services.camperservice.features.mealplan.actions.UpdatePurchaseAction
import com.acme.services.camperservice.features.mealplan.params.*

class MealPlanService(
    mealPlanClient: MealPlanClient,
    recipeClient: RecipeClient,
    ingredientClient: IngredientClient,
) {
    private val createMealPlan = CreateMealPlanAction(mealPlanClient)
    private val getMealPlanDetail = GetMealPlanDetailAction(mealPlanClient, recipeClient, ingredientClient)
    private val getMealPlanByPlanId = GetMealPlanByPlanIdAction(mealPlanClient, recipeClient, ingredientClient)
    private val getTemplatesAction = GetTemplatesAction(mealPlanClient)
    private val updateMealPlan = UpdateMealPlanAction(mealPlanClient)
    private val deleteMealPlan = DeleteMealPlanAction(mealPlanClient)
    private val copyToTrip = CopyToTripAction(mealPlanClient, recipeClient, ingredientClient)
    private val saveAsTemplate = SaveAsTemplateAction(mealPlanClient)
    private val addDay = AddDayAction(mealPlanClient)
    private val removeDay = RemoveDayAction(mealPlanClient)
    private val addRecipeToMeal = AddRecipeToMealAction(mealPlanClient, recipeClient, ingredientClient)
    private val removeRecipeFromMeal = RemoveRecipeFromMealAction(mealPlanClient)
    private val getShoppingList = GetShoppingListAction(mealPlanClient, recipeClient, ingredientClient)
    private val updatePurchase = UpdatePurchaseAction(mealPlanClient)
    private val resetPurchases = ResetPurchasesAction(mealPlanClient)
    private val addManualItemAction = AddManualItemAction(mealPlanClient, ingredientClient)
    private val removeManualItemAction = RemoveManualItemAction(mealPlanClient)

    fun create(param: CreateMealPlanParam) = createMealPlan.execute(param)
    fun getDetail(param: GetMealPlanDetailParam) = getMealPlanDetail.execute(param)
    fun getByPlanId(param: GetMealPlanByPlanIdParam) = getMealPlanByPlanId.execute(param)
    fun getTemplates(param: GetTemplatesParam) = getTemplatesAction.execute(param)
    fun update(param: UpdateMealPlanParam) = updateMealPlan.execute(param)
    fun delete(param: DeleteMealPlanParam) = deleteMealPlan.execute(param)
    fun copyToTrip(param: CopyToTripParam) = copyToTrip.execute(param)
    fun saveAsTemplate(param: SaveAsTemplateParam) = saveAsTemplate.execute(param)
    fun addDay(param: AddDayParam) = addDay.execute(param)
    fun removeDay(param: RemoveDayParam) = removeDay.execute(param)
    fun addRecipeToMeal(param: AddRecipeToMealParam) = addRecipeToMeal.execute(param)
    fun removeRecipeFromMeal(param: RemoveRecipeFromMealParam) = removeRecipeFromMeal.execute(param)
    fun getShoppingList(param: GetShoppingListParam) = getShoppingList.execute(param)
    fun updatePurchase(param: UpdatePurchaseParam) = updatePurchase.execute(param)
    fun resetPurchases(param: ResetPurchasesParam) = resetPurchases.execute(param)
    fun addManualItem(param: AddManualItemParam) = addManualItemAction.execute(param)
    fun removeManualItem(param: RemoveManualItemParam) = removeManualItemAction.execute(param)
}
