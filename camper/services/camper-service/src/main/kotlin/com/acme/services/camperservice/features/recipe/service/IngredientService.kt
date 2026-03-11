package com.acme.services.camperservice.features.recipe.service

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.recipe.actions.CreateIngredientAction
import com.acme.services.camperservice.features.recipe.actions.DeleteIngredientAction
import com.acme.services.camperservice.features.recipe.actions.ListIngredientsAction
import com.acme.services.camperservice.features.recipe.actions.UpdateIngredientAction
import com.acme.services.camperservice.features.recipe.params.CreateIngredientParam
import com.acme.services.camperservice.features.recipe.params.DeleteIngredientParam
import com.acme.services.camperservice.features.recipe.params.ListIngredientsParam
import com.acme.services.camperservice.features.recipe.params.UpdateIngredientParam

class IngredientService(
    ingredientClient: IngredientClient,
    recipeClient: RecipeClient
) {
    private val createIngredient = CreateIngredientAction(ingredientClient)
    private val listIngredients = ListIngredientsAction(ingredientClient)
    private val updateIngredient = UpdateIngredientAction(ingredientClient)
    private val deleteIngredient = DeleteIngredientAction(ingredientClient, recipeClient)

    fun create(param: CreateIngredientParam) = createIngredient.execute(param)
    fun list(param: ListIngredientsParam) = listIngredients.execute(param)
    fun update(param: UpdateIngredientParam) = updateIngredient.execute(param)
    fun delete(param: DeleteIngredientParam) = deleteIngredient.execute(param)
}
