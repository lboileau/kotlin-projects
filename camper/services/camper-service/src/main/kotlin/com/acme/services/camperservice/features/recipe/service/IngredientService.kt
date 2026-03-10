package com.acme.services.camperservice.features.recipe.service

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.services.camperservice.features.recipe.actions.CreateIngredientAction
import com.acme.services.camperservice.features.recipe.actions.ListIngredientsAction
import com.acme.services.camperservice.features.recipe.actions.UpdateIngredientAction
import com.acme.services.camperservice.features.recipe.params.CreateIngredientParam
import com.acme.services.camperservice.features.recipe.params.ListIngredientsParam
import com.acme.services.camperservice.features.recipe.params.UpdateIngredientParam

class IngredientService(
    ingredientClient: IngredientClient
) {
    private val createIngredient = CreateIngredientAction(ingredientClient)
    private val listIngredients = ListIngredientsAction(ingredientClient)
    private val updateIngredient = UpdateIngredientAction(ingredientClient)

    fun create(param: CreateIngredientParam) = createIngredient.execute(param)
    fun list(param: ListIngredientsParam) = listIngredients.execute(param)
    fun update(param: UpdateIngredientParam) = updateIngredient.execute(param)
}
