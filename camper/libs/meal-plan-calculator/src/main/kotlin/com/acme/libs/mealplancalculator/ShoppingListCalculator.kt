package com.acme.libs.mealplancalculator

import com.acme.libs.mealplancalculator.model.IngredientInfo
import com.acme.libs.mealplancalculator.model.RecipeIngredientWithMeta
import com.acme.libs.mealplancalculator.model.ScaledIngredient
import com.acme.libs.mealplancalculator.model.ScalingMode
import com.acme.libs.mealplancalculator.model.ShoppingListRow
import com.acme.libs.mealplancalculator.model.UnitCategory
import java.math.BigDecimal
import java.math.RoundingMode
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

    private const val INTERMEDIATE_SCALE = 10

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
    ): List<ScaledIngredient> {
        val targetServings = BigDecimal(servings)

        return recipeIngredients.map { ingredient ->
            val baseServings = BigDecimal(ingredient.baseServings)
            val scaleFactor = when (scalingMode) {
                ScalingMode.FRACTIONAL -> targetServings.divide(baseServings, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
                ScalingMode.ROUND_UP -> targetServings.divide(baseServings, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.CEILING)
            }
            val scaledQuantity = ingredient.quantity.multiply(scaleFactor)
                .setScale(INTERMEDIATE_SCALE, RoundingMode.HALF_UP)

            ScaledIngredient(
                ingredientId = ingredient.ingredientId,
                recipeIngredientId = ingredient.recipeIngredientId,
                recipeName = ingredient.recipeName,
                quantity = scaledQuantity,
                unit = ingredient.unit,
            )
        }
    }

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
    ): List<ShoppingListRow> {
        val byIngredient = scaledIngredients.groupBy { it.ingredientId }
        val rows = mutableListOf<ShoppingListRow>()

        for ((ingredientId, ingredients) in byIngredient) {
            val info = ingredientLookup[ingredientId]
            val ingredientName = info?.name ?: "Unknown"
            val category = info?.category ?: "uncategorized"

            // Partition into compatible unit subgroups
            val subgroups = partitionIntoSubgroups(ingredients)

            for (subgroup in subgroups) {
                val recipeNames = subgroup.map { it.recipeName }.distinct().sorted()

                // Pick the first unit in the subgroup as the common unit
                val commonUnit = subgroup.first().unit
                var total = BigDecimal.ZERO.setScale(INTERMEDIATE_SCALE)

                for (item in subgroup) {
                    val converted = UnitConverter.convert(item.quantity, item.unit, commonUnit)
                        ?: item.quantity // Fallback: shouldn't happen within a compatible subgroup
                    total = total.add(converted)
                }

                val (bestQuantity, bestUnit) = UnitConverter.bestFit(total, commonUnit)

                rows.add(
                    ShoppingListRow(
                        ingredientId = ingredientId,
                        ingredientName = ingredientName,
                        category = category,
                        quantityRequired = bestQuantity,
                        unit = bestUnit,
                        usedInRecipes = recipeNames,
                    ),
                )
            }
        }

        return rows
    }

    /**
     * Partition a list of scaled ingredients (all for the same ingredient ID) into
     * subgroups of compatible units:
     * - All volume units together
     * - All weight units together
     * - Each count unit in its own subgroup
     */
    private fun partitionIntoSubgroups(ingredients: List<ScaledIngredient>): List<List<ScaledIngredient>> {
        val volumeGroup = mutableListOf<ScaledIngredient>()
        val weightGroup = mutableListOf<ScaledIngredient>()
        val countGroups = mutableMapOf<String, MutableList<ScaledIngredient>>()

        for (ingredient in ingredients) {
            when (UnitConverter.categoryOf(ingredient.unit)) {
                UnitCategory.VOLUME -> volumeGroup.add(ingredient)
                UnitCategory.WEIGHT -> weightGroup.add(ingredient)
                UnitCategory.COUNT -> countGroups.getOrPut(ingredient.unit) { mutableListOf() }.add(ingredient)
                null -> countGroups.getOrPut(ingredient.unit) { mutableListOf() }.add(ingredient)
            }
        }

        val result = mutableListOf<List<ScaledIngredient>>()
        if (volumeGroup.isNotEmpty()) result.add(volumeGroup)
        if (weightGroup.isNotEmpty()) result.add(weightGroup)
        for (group in countGroups.values) {
            result.add(group)
        }
        return result
    }
}
