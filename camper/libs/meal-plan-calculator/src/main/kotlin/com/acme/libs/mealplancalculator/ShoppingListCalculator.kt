package com.acme.libs.mealplancalculator

import com.acme.libs.mealplancalculator.model.IngredientInfo
import com.acme.libs.mealplancalculator.model.RecipeIngredientWithMeta
import com.acme.libs.mealplancalculator.model.ScaledIngredient
import com.acme.libs.mealplancalculator.model.ScalingMode
import com.acme.libs.mealplancalculator.model.ShoppingListRow
import java.util.UUID

/**
 * Computes scaled ingredient quantities and aggregates them into a shopping list.
 *
 * The shopping list is fully computed at read time:
 * 1. Scale each recipe ingredient by the meal plan's servings and scaling mode
 * 2. Group by ingredient, partition into compatible unit subgroups
 * 3. Within each subgroup, convert to a common unit, sum, then best-fit the total
 * 4. Each subgroup becomes one shopping list row
 */
object ShoppingListCalculator {

    /**
     * Scale recipe ingredients based on the meal plan's target servings and scaling mode.
     *
     * Each [RecipeIngredientWithMeta] carries its own [RecipeIngredientWithMeta.baseServings]
     * because different recipes in the same meal plan can have different base serving sizes.
     *
     * - **Fractional**: quantity * (servings / baseServings)
     * - **Round-up**: quantity * ceil(servings / baseServings)
     */
    fun scaleIngredients(
        recipeIngredients: List<RecipeIngredientWithMeta>,
        servings: Int,
        scalingMode: ScalingMode,
    ): List<ScaledIngredient> = TODO()

    /**
     * Aggregate scaled ingredients into shopping list rows.
     *
     * Groups by ingredient ID, then partitions each group into compatible unit subgroups
     * (volume, weight, or individual count units). Within each subgroup, converts all
     * quantities to a common unit, sums them, and applies best-fit to the total.
     */
    fun aggregateShoppingList(
        scaledIngredients: List<ScaledIngredient>,
        ingredientLookup: Map<UUID, IngredientInfo>,
    ): List<ShoppingListRow> = TODO()
}
