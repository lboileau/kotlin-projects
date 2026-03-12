package com.acme.services.camperservice.features.mealplan.service

import com.acme.services.camperservice.features.mealplan.actions.AddDayAction
import com.acme.services.camperservice.features.mealplan.actions.AddRecipeToMealAction
import com.acme.services.camperservice.features.mealplan.actions.CopyToTripAction
import com.acme.services.camperservice.features.mealplan.actions.CreateMealPlanAction
import com.acme.services.camperservice.features.mealplan.actions.DeleteMealPlanAction
import com.acme.services.camperservice.features.mealplan.actions.GetMealPlanByPlanIdAction
import com.acme.services.camperservice.features.mealplan.actions.GetMealPlanDetailAction
import com.acme.services.camperservice.features.mealplan.actions.GetShoppingListAction
import com.acme.services.camperservice.features.mealplan.actions.GetTemplatesAction
import com.acme.services.camperservice.features.mealplan.actions.RemoveDayAction
import com.acme.services.camperservice.features.mealplan.actions.RemoveRecipeFromMealAction
import com.acme.services.camperservice.features.mealplan.actions.ResetPurchasesAction
import com.acme.services.camperservice.features.mealplan.actions.SaveAsTemplateAction
import com.acme.services.camperservice.features.mealplan.actions.UpdateMealPlanAction
import com.acme.services.camperservice.features.mealplan.actions.UpdatePurchaseAction
import com.acme.services.camperservice.features.mealplan.params.*

class MealPlanService {
    private val createMealPlan = CreateMealPlanAction()
    private val getMealPlanDetail = GetMealPlanDetailAction()
    private val getMealPlanByPlanId = GetMealPlanByPlanIdAction()
    private val getTemplatesAction = GetTemplatesAction()
    private val updateMealPlan = UpdateMealPlanAction()
    private val deleteMealPlan = DeleteMealPlanAction()
    private val copyToTrip = CopyToTripAction()
    private val saveAsTemplate = SaveAsTemplateAction()
    private val addDay = AddDayAction()
    private val removeDay = RemoveDayAction()
    private val addRecipeToMeal = AddRecipeToMealAction()
    private val removeRecipeFromMeal = RemoveRecipeFromMealAction()
    private val getShoppingList = GetShoppingListAction()
    private val updatePurchase = UpdatePurchaseAction()
    private val resetPurchases = ResetPurchasesAction()

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
}
