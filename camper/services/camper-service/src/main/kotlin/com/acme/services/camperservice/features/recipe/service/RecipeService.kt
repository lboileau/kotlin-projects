package com.acme.services.camperservice.features.recipe.service

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.services.camperservice.features.recipe.actions.CreateRecipeAction
import com.acme.services.camperservice.features.recipe.actions.DeleteRecipeAction
import com.acme.services.camperservice.features.recipe.actions.GetRecipeAction
import com.acme.services.camperservice.features.recipe.actions.ImportRecipeAction
import com.acme.services.camperservice.features.recipe.actions.ListRecipesAction
import com.acme.services.camperservice.features.recipe.actions.PublishRecipeAction
import com.acme.services.camperservice.features.recipe.actions.ResolveDuplicateAction
import com.acme.services.camperservice.features.recipe.actions.ResolveIngredientAction
import com.acme.services.camperservice.features.recipe.actions.UpdateRecipeAction
import com.acme.services.camperservice.features.recipe.params.CreateRecipeParam
import com.acme.services.camperservice.features.recipe.params.DeleteRecipeParam
import com.acme.services.camperservice.features.recipe.params.GetRecipeParam
import com.acme.services.camperservice.features.recipe.params.ImportRecipeParam
import com.acme.services.camperservice.features.recipe.params.ListRecipesParam
import com.acme.services.camperservice.features.recipe.params.PublishRecipeParam
import com.acme.services.camperservice.features.recipe.params.ResolveDuplicateParam
import com.acme.services.camperservice.features.recipe.params.ResolveIngredientParam
import com.acme.services.camperservice.features.recipe.params.UpdateRecipeParam

class RecipeService(
    recipeClient: RecipeClient,
    ingredientClient: IngredientClient,
    recipeScraperClient: RecipeScraperClient
) {
    private val createRecipe = CreateRecipeAction(recipeClient, ingredientClient)
    private val importRecipe = ImportRecipeAction(recipeClient, ingredientClient, recipeScraperClient)
    private val getRecipe = GetRecipeAction(recipeClient, ingredientClient)
    private val listRecipes = ListRecipesAction(recipeClient)
    private val updateRecipe = UpdateRecipeAction(recipeClient)
    private val deleteRecipe = DeleteRecipeAction(recipeClient)
    private val resolveIngredient = ResolveIngredientAction(recipeClient, ingredientClient)
    private val resolveDuplicate = ResolveDuplicateAction(recipeClient)
    private val publishRecipe = PublishRecipeAction(recipeClient)

    fun create(param: CreateRecipeParam) = createRecipe.execute(param)
    fun import(param: ImportRecipeParam) = importRecipe.execute(param)
    fun get(param: GetRecipeParam) = getRecipe.execute(param)
    fun list(param: ListRecipesParam) = listRecipes.execute(param)
    fun update(param: UpdateRecipeParam) = updateRecipe.execute(param)
    fun delete(param: DeleteRecipeParam) = deleteRecipe.execute(param)
    fun resolveIngredient(param: ResolveIngredientParam) = resolveIngredient.execute(param)
    fun resolveDuplicate(param: ResolveDuplicateParam) = resolveDuplicate.execute(param)
    fun publish(param: PublishRecipeParam) = publishRecipe.execute(param)
}
