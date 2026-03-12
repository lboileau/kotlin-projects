package com.acme.libs.mealplancalculator

import com.acme.libs.mealplancalculator.model.IngredientInfo
import com.acme.libs.mealplancalculator.model.PurchaseStatus
import com.acme.libs.mealplancalculator.model.RecipeIngredientWithMeta
import com.acme.libs.mealplancalculator.model.ScaledIngredient
import com.acme.libs.mealplancalculator.model.ScalingMode
import com.acme.libs.mealplancalculator.model.ShoppingListRow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class ShoppingListCalculatorTest {

    companion object {
        private val CARROT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val ONION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002")
        private val FLOUR_ID = UUID.fromString("00000000-0000-0000-0000-000000000003")
        private val GARLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000004")
        private val SALT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005")

        private val INGREDIENT_LOOKUP = mapOf(
            CARROT_ID to IngredientInfo(CARROT_ID, "Carrots", "produce"),
            ONION_ID to IngredientInfo(ONION_ID, "Onions", "produce"),
            FLOUR_ID to IngredientInfo(FLOUR_ID, "Flour", "pantry"),
            GARLIC_ID to IngredientInfo(GARLIC_ID, "Garlic", "produce"),
            SALT_ID to IngredientInfo(SALT_ID, "Salt", "pantry"),
        )

        private fun recipeIngredient(
            ingredientId: UUID,
            name: String,
            quantity: String,
            unit: String,
            recipeName: String,
            baseServings: Int,
        ) = RecipeIngredientWithMeta(
            recipeIngredientId = UUID.randomUUID(),
            ingredientId = ingredientId,
            ingredientName = name,
            category = "produce",
            quantity = BigDecimal(quantity),
            unit = unit,
            recipeName = recipeName,
            baseServings = baseServings,
        )

        private fun scaledIngredient(
            ingredientId: UUID,
            quantity: String,
            unit: String,
            recipeName: String,
        ) = ScaledIngredient(
            ingredientId = ingredientId,
            recipeIngredientId = UUID.randomUUID(),
            recipeName = recipeName,
            quantity = BigDecimal(quantity),
            unit = unit,
        )
    }

    @Nested
    inner class ScaleIngredients {

        @Test
        fun `fractional scaling multiplies by exact ratio`() {
            val ingredients = listOf(
                recipeIngredient(CARROT_ID, "Carrots", "1", "cup", "Chili", 4),
                recipeIngredient(ONION_ID, "Onions", "2", "whole", "Chili", 4),
            )

            val result = ShoppingListCalculator.scaleIngredients(ingredients, 6, ScalingMode.FRACTIONAL)

            assertThat(result).hasSize(2)
            assertThat(result[0].quantity).isEqualByComparingTo(BigDecimal("1.5"))
            assertThat(result[0].unit).isEqualTo("cup")
            assertThat(result[1].quantity).isEqualByComparingTo(BigDecimal("3"))
            assertThat(result[1].unit).isEqualTo("whole")
        }

        @Test
        fun `round-up scaling uses ceiling of ratio`() {
            // 6/4 = 1.5 → ceil = 2
            val ingredients = listOf(
                recipeIngredient(CARROT_ID, "Carrots", "1", "cup", "Chili", 4),
                recipeIngredient(ONION_ID, "Onions", "2", "whole", "Chili", 4),
            )

            val result = ShoppingListCalculator.scaleIngredients(ingredients, 6, ScalingMode.ROUND_UP)

            assertThat(result).hasSize(2)
            assertThat(result[0].quantity).isEqualByComparingTo(BigDecimal("2"))
            assertThat(result[1].quantity).isEqualByComparingTo(BigDecimal("4"))
        }

        @Test
        fun `round-up exact fit does not over-round`() {
            // 8/4 = 2.0 → ceil = 2
            val ingredients = listOf(
                recipeIngredient(FLOUR_ID, "Flour", "3", "cup", "Pancakes", 4),
            )

            val result = ShoppingListCalculator.scaleIngredients(ingredients, 8, ScalingMode.ROUND_UP)

            assertThat(result).hasSize(1)
            assertThat(result[0].quantity).isEqualByComparingTo(BigDecimal("6"))
        }

        @Test
        fun `scale factor less than 1 fractional`() {
            // 2/4 = 0.5
            val ingredients = listOf(
                recipeIngredient(FLOUR_ID, "Flour", "4", "cup", "Pancakes", 4),
            )

            val result = ShoppingListCalculator.scaleIngredients(ingredients, 2, ScalingMode.FRACTIONAL)

            assertThat(result).hasSize(1)
            assertThat(result[0].quantity).isEqualByComparingTo(BigDecimal("2"))
        }

        @Test
        fun `scale factor less than 1 round-up`() {
            // 2/4 = 0.5 → ceil = 1
            val ingredients = listOf(
                recipeIngredient(FLOUR_ID, "Flour", "4", "cup", "Pancakes", 4),
            )

            val result = ShoppingListCalculator.scaleIngredients(ingredients, 2, ScalingMode.ROUND_UP)

            assertThat(result).hasSize(1)
            assertThat(result[0].quantity).isEqualByComparingTo(BigDecimal("4"))
        }

        @Test
        fun `scale factor equals 1`() {
            val ingredients = listOf(
                recipeIngredient(FLOUR_ID, "Flour", "2", "cup", "Pancakes", 4),
            )

            val result = ShoppingListCalculator.scaleIngredients(ingredients, 4, ScalingMode.FRACTIONAL)

            assertThat(result).hasSize(1)
            assertThat(result[0].quantity).isEqualByComparingTo(BigDecimal("2"))
        }

        @Test
        fun `large scale factor`() {
            // 20/2 = 10x
            val ingredients = listOf(
                recipeIngredient(SALT_ID, "Salt", "1", "tsp", "Stir Fry", 2),
            )

            val result = ShoppingListCalculator.scaleIngredients(ingredients, 20, ScalingMode.FRACTIONAL)

            assertThat(result).hasSize(1)
            assertThat(result[0].quantity).isEqualByComparingTo(BigDecimal("10"))
        }

        @Test
        fun `multiple recipes with different base servings`() {
            val ingredients = listOf(
                recipeIngredient(CARROT_ID, "Carrots", "1", "cup", "Chili", 4),
                recipeIngredient(CARROT_ID, "Carrots", "2", "tbsp", "Stir Fry", 2),
            )

            // servings=6: Chili scale = 6/4 = 1.5, Stir Fry scale = 6/2 = 3
            val result = ShoppingListCalculator.scaleIngredients(ingredients, 6, ScalingMode.FRACTIONAL)

            assertThat(result).hasSize(2)
            assertThat(result[0].quantity).isEqualByComparingTo(BigDecimal("1.5")) // 1 * 1.5
            assertThat(result[0].recipeName).isEqualTo("Chili")
            assertThat(result[1].quantity).isEqualByComparingTo(BigDecimal("6")) // 2 * 3
            assertThat(result[1].recipeName).isEqualTo("Stir Fry")
        }

        @Test
        fun `preserves ingredient metadata`() {
            val ingredients = listOf(
                recipeIngredient(CARROT_ID, "Carrots", "1", "cup", "Chili", 4),
            )

            val result = ShoppingListCalculator.scaleIngredients(ingredients, 6, ScalingMode.FRACTIONAL)

            assertThat(result[0].ingredientId).isEqualTo(CARROT_ID)
            assertThat(result[0].recipeName).isEqualTo("Chili")
            assertThat(result[0].unit).isEqualTo("cup")
        }

        @Test
        fun `empty input returns empty list`() {
            val result = ShoppingListCalculator.scaleIngredients(emptyList(), 6, ScalingMode.FRACTIONAL)
            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class AggregateShoppingList {

        @Test
        fun `same ingredient same unit sums quantities`() {
            val scaled = listOf(
                scaledIngredient(FLOUR_ID, "2", "cup", "Recipe A"),
                scaledIngredient(FLOUR_ID, "1", "cup", "Recipe B"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(1)
            assertThat(result[0].ingredientName).isEqualTo("Flour")
            assertThat(result[0].quantityRequired).isEqualByComparingTo(BigDecimal("3"))
            assertThat(result[0].unit).isEqualTo("cup")
        }

        @Test
        fun `same ingredient compatible units converts and sums`() {
            val scaled = listOf(
                scaledIngredient(CARROT_ID, "1.5", "cup", "Chili"),
                scaledIngredient(CARROT_ID, "6", "tbsp", "Stir Fry"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            // Both are volume: 1.5 cups + 6 tbsp
            // 6 tbsp = 6*3/48 cup = 0.375 cup
            // total = 1.875 cups (bestFit should keep as cups since 1.875 is not clean for any larger unit)
            assertThat(result).hasSize(1)
            assertThat(result[0].ingredientName).isEqualTo("Carrots")
            assertThat(result[0].quantityRequired).isEqualByComparingTo(BigDecimal("1.875"))
            assertThat(result[0].unit).isEqualTo("cup")
        }

        @Test
        fun `same ingredient incompatible units creates separate rows`() {
            val scaled = listOf(
                scaledIngredient(CARROT_ID, "1.5", "cup", "Chili"),
                scaledIngredient(CARROT_ID, "3", "whole", "Stir Fry"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(2)
            val volumeRow = result.find { it.unit == "cup" }!!
            val countRow = result.find { it.unit == "whole" }!!
            assertThat(volumeRow.quantityRequired).isEqualByComparingTo(BigDecimal("1.5"))
            assertThat(countRow.quantityRequired).isEqualByComparingTo(BigDecimal("3"))
        }

        @Test
        fun `same ingredient three unit groups creates three rows`() {
            val scaled = listOf(
                scaledIngredient(CARROT_ID, "1", "cup", "Soup"),
                scaledIngredient(CARROT_ID, "500", "g", "Stew"),
                scaledIngredient(CARROT_ID, "2", "whole", "Salad"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(3)
            // 500g bestFits to 0.5 kg
            assertThat(result.map { it.unit }.toSet()).containsExactlyInAnyOrder("cup", "kg", "whole")
        }

        @Test
        fun `single recipe single ingredient no aggregation needed`() {
            val scaled = listOf(
                scaledIngredient(FLOUR_ID, "2", "cup", "Pancakes"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(1)
            assertThat(result[0].quantityRequired).isEqualByComparingTo(BigDecimal("2"))
            assertThat(result[0].unit).isEqualTo("cup")
            assertThat(result[0].usedInRecipes).containsExactly("Pancakes")
        }

        @Test
        fun `many recipes same ingredient correct total`() {
            val scaled = listOf(
                scaledIngredient(ONION_ID, "2", "whole", "Chili"),
                scaledIngredient(ONION_ID, "1", "whole", "Soup"),
                scaledIngredient(ONION_ID, "3", "whole", "Stir Fry"),
                scaledIngredient(ONION_ID, "1", "whole", "Salad"),
                scaledIngredient(ONION_ID, "2", "whole", "Stew"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(1)
            assertThat(result[0].quantityRequired).isEqualByComparingTo(BigDecimal("9"))
            assertThat(result[0].unit).isEqualTo("whole")
        }

        @Test
        fun `empty input returns empty list`() {
            val result = ShoppingListCalculator.aggregateShoppingList(emptyList(), INGREDIENT_LOOKUP)
            assertThat(result).isEmpty()
        }

        @Test
        fun `recipe names are deduplicated and sorted`() {
            val scaled = listOf(
                scaledIngredient(FLOUR_ID, "1", "cup", "Pancakes"),
                scaledIngredient(FLOUR_ID, "2", "cup", "Bread"),
                scaledIngredient(FLOUR_ID, "1", "cup", "Pancakes"), // duplicate recipe name
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(1)
            assertThat(result[0].usedInRecipes).containsExactly("Bread", "Pancakes")
        }

        @Test
        fun `ingredient lookup provides correct name and category`() {
            val scaled = listOf(
                scaledIngredient(GARLIC_ID, "3", "clove", "Stir Fry"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(1)
            assertThat(result[0].ingredientName).isEqualTo("Garlic")
            assertThat(result[0].category).isEqualTo("produce")
        }

        @Test
        fun `missing ingredient lookup uses defaults`() {
            val unknownId = UUID.randomUUID()
            val scaled = listOf(
                scaledIngredient(unknownId, "1", "cup", "Mystery Dish"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(1)
            assertThat(result[0].ingredientName).isEqualTo("Unknown")
            assertThat(result[0].category).isEqualTo("uncategorized")
        }

        @Test
        fun `compatible units apply bestFit to result`() {
            // 48 tsp + 0 = 48 tsp → bestFit → 1 cup
            val scaled = listOf(
                scaledIngredient(FLOUR_ID, "24", "tsp", "Recipe A"),
                scaledIngredient(FLOUR_ID, "24", "tsp", "Recipe B"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(1)
            assertThat(result[0].quantityRequired).isEqualByComparingTo(BigDecimal("1"))
            assertThat(result[0].unit).isEqualTo("cup")
        }

        @Test
        fun `different count units for same ingredient create separate rows`() {
            val scaled = listOf(
                scaledIngredient(GARLIC_ID, "3", "clove", "Stir Fry"),
                scaledIngredient(GARLIC_ID, "1", "whole", "Roast"),
            )

            val result = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(result).hasSize(2)
            val cloveRow = result.find { it.unit == "clove" }!!
            val wholeRow = result.find { it.unit == "whole" }!!
            assertThat(cloveRow.quantityRequired).isEqualByComparingTo(BigDecimal("3"))
            assertThat(wholeRow.quantityRequired).isEqualByComparingTo(BigDecimal("1"))
        }
    }

    @Nested
    inner class FullPipelineIntegration {

        @Test
        fun `plan example - chili and stir fry with 6 servings fractional`() {
            // From the plan: Trip has 6 people, scaling mode = fractional
            // Chili (base 4 servings): 1 cup carrots, 2 whole onions
            // Stir Fry (base 2 servings): 2 tbsp carrots, 1 whole carrot, 3 whole onions

            val ingredients = listOf(
                recipeIngredient(CARROT_ID, "Carrots", "1", "cup", "Chili", 4),
                recipeIngredient(ONION_ID, "Onions", "2", "whole", "Chili", 4),
                recipeIngredient(CARROT_ID, "Carrots", "2", "tbsp", "Stir Fry", 2),
                recipeIngredient(CARROT_ID, "Carrots", "1", "whole", "Stir Fry", 2),
                recipeIngredient(ONION_ID, "Onions", "3", "whole", "Stir Fry", 2),
            )

            val scaled = ShoppingListCalculator.scaleIngredients(ingredients, 6, ScalingMode.FRACTIONAL)

            // Verify scaling
            // Chili: scale = 6/4 = 1.5x → 1.5 cups carrots, 3 whole onions
            // Stir Fry: scale = 6/2 = 3x → 6 tbsp carrots, 3 whole carrots, 9 whole onions
            val chiliCarrotCup = scaled.find { it.recipeName == "Chili" && it.unit == "cup" }!!
            assertThat(chiliCarrotCup.quantity).isEqualByComparingTo(BigDecimal("1.5"))

            val chiliOnion = scaled.find { it.recipeName == "Chili" && it.ingredientId == ONION_ID }!!
            assertThat(chiliOnion.quantity).isEqualByComparingTo(BigDecimal("3"))

            val stirFryCarrotTbsp = scaled.find { it.recipeName == "Stir Fry" && it.unit == "tbsp" }!!
            assertThat(stirFryCarrotTbsp.quantity).isEqualByComparingTo(BigDecimal("6"))

            val stirFryCarrotWhole = scaled.find { it.recipeName == "Stir Fry" && it.unit == "whole" && it.ingredientId == CARROT_ID }!!
            assertThat(stirFryCarrotWhole.quantity).isEqualByComparingTo(BigDecimal("3"))

            val stirFryOnion = scaled.find { it.recipeName == "Stir Fry" && it.ingredientId == ONION_ID }!!
            assertThat(stirFryOnion.quantity).isEqualByComparingTo(BigDecimal("9"))

            // Aggregate
            val rows = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            // Expected:
            // Carrots (volume group): 1.5 cups + 6 tbsp = 1.5 + 0.375 = 1.875 cups
            // Carrots (count group): 3 whole
            // Onions (count group): 3 + 9 = 12 whole
            val carrotVolumeRow = rows.find { it.ingredientId == CARROT_ID && it.unit == "cup" }!!
            assertThat(carrotVolumeRow.quantityRequired).isEqualByComparingTo(BigDecimal("1.875"))
            assertThat(carrotVolumeRow.usedInRecipes).containsExactly("Chili", "Stir Fry")

            val carrotCountRow = rows.find { it.ingredientId == CARROT_ID && it.unit == "whole" }!!
            assertThat(carrotCountRow.quantityRequired).isEqualByComparingTo(BigDecimal("3"))
            assertThat(carrotCountRow.usedInRecipes).containsExactly("Stir Fry")

            val onionRow = rows.find { it.ingredientId == ONION_ID }!!
            assertThat(onionRow.quantityRequired).isEqualByComparingTo(BigDecimal("12"))
            assertThat(onionRow.unit).isEqualTo("whole")
            assertThat(onionRow.usedInRecipes).containsExactly("Chili", "Stir Fry")
        }

        @Test
        fun `plan example with round-up scaling`() {
            val ingredients = listOf(
                recipeIngredient(CARROT_ID, "Carrots", "1", "cup", "Chili", 4),
                recipeIngredient(ONION_ID, "Onions", "2", "whole", "Chili", 4),
                recipeIngredient(CARROT_ID, "Carrots", "2", "tbsp", "Stir Fry", 2),
                recipeIngredient(CARROT_ID, "Carrots", "1", "whole", "Stir Fry", 2),
                recipeIngredient(ONION_ID, "Onions", "3", "whole", "Stir Fry", 2),
            )

            // Chili: ceil(6/4) = ceil(1.5) = 2x
            // Stir Fry: ceil(6/2) = ceil(3) = 3x
            val scaled = ShoppingListCalculator.scaleIngredients(ingredients, 6, ScalingMode.ROUND_UP)
            val rows = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            // Carrots volume: 2 cups (Chili 2x) + 6 tbsp (Stir Fry 3x) = 2 + 0.375 = 2.375 cups
            val carrotVolumeRow = rows.find { it.ingredientId == CARROT_ID && it.unit == "cup" }!!
            assertThat(carrotVolumeRow.quantityRequired).isEqualByComparingTo(BigDecimal("2.375"))

            // Carrots count: 3 whole (Stir Fry 3x)
            val carrotCountRow = rows.find { it.ingredientId == CARROT_ID && it.unit == "whole" }!!
            assertThat(carrotCountRow.quantityRequired).isEqualByComparingTo(BigDecimal("3"))

            // Onions: 4 (Chili 2x) + 9 (Stir Fry 3x) = 13 whole
            val onionRow = rows.find { it.ingredientId == ONION_ID }!!
            assertThat(onionRow.quantityRequired).isEqualByComparingTo(BigDecimal("13"))
        }

        @Test
        fun `same recipe on multiple days ingredients counted twice`() {
            // Pancakes on day 1 and day 3 — flour counted twice
            val ingredients = listOf(
                recipeIngredient(FLOUR_ID, "Flour", "2", "cup", "Pancakes", 4),
                recipeIngredient(FLOUR_ID, "Flour", "2", "cup", "Pancakes", 4),
            )

            val scaled = ShoppingListCalculator.scaleIngredients(ingredients, 4, ScalingMode.FRACTIONAL)
            val rows = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(rows).hasSize(1)
            assertThat(rows[0].quantityRequired).isEqualByComparingTo(BigDecimal("4"))
            assertThat(rows[0].usedInRecipes).containsExactly("Pancakes") // deduplicated
        }

        @Test
        fun `same recipe on same day different meals ingredients counted twice`() {
            // Pancakes for breakfast AND snack
            val ingredients = listOf(
                recipeIngredient(FLOUR_ID, "Flour", "2", "cup", "Pancakes", 4),
                recipeIngredient(FLOUR_ID, "Flour", "2", "cup", "Pancakes", 4),
            )

            val scaled = ShoppingListCalculator.scaleIngredients(ingredients, 8, ScalingMode.FRACTIONAL)
            val rows = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            // scale = 8/4 = 2x → each instance = 4 cups → total = 8 cups
            assertThat(rows).hasSize(1)
            assertThat(rows[0].quantityRequired).isEqualByComparingTo(BigDecimal("8"))
        }

        @Test
        fun `headcount increase doubles required quantities`() {
            val ingredients = listOf(
                recipeIngredient(FLOUR_ID, "Flour", "3", "cup", "Pancakes", 4),
            )

            val scaled4 = ShoppingListCalculator.scaleIngredients(ingredients, 4, ScalingMode.FRACTIONAL)
            val rows4 = ShoppingListCalculator.aggregateShoppingList(scaled4, INGREDIENT_LOOKUP)
            assertThat(rows4[0].quantityRequired).isEqualByComparingTo(BigDecimal("3"))

            val scaled8 = ShoppingListCalculator.scaleIngredients(ingredients, 8, ScalingMode.FRACTIONAL)
            val rows8 = ShoppingListCalculator.aggregateShoppingList(scaled8, INGREDIENT_LOOKUP)
            assertThat(rows8[0].quantityRequired).isEqualByComparingTo(BigDecimal("6"))
        }

        @Test
        fun `scaling mode change recomputes quantities`() {
            val ingredients = listOf(
                recipeIngredient(FLOUR_ID, "Flour", "3", "cup", "Pancakes", 4),
            )

            // Fractional: 6/4 = 1.5 → 4.5 cups
            val fractional = ShoppingListCalculator.scaleIngredients(ingredients, 6, ScalingMode.FRACTIONAL)
            val fractionalRows = ShoppingListCalculator.aggregateShoppingList(fractional, INGREDIENT_LOOKUP)
            assertThat(fractionalRows[0].quantityRequired).isEqualByComparingTo(BigDecimal("4.5"))

            // Round-up: ceil(6/4) = 2 → 6 cups
            val roundUp = ShoppingListCalculator.scaleIngredients(ingredients, 6, ScalingMode.ROUND_UP)
            val roundUpRows = ShoppingListCalculator.aggregateShoppingList(roundUp, INGREDIENT_LOOKUP)
            assertThat(roundUpRows[0].quantityRequired).isEqualByComparingTo(BigDecimal("6"))
        }

        @Test
        fun `multiple distinct ingredients aggregate independently`() {
            val scaled = listOf(
                scaledIngredient(FLOUR_ID, "3", "cup", "Pancakes"),
                scaledIngredient(SALT_ID, "1", "tsp", "Pancakes"),
                scaledIngredient(FLOUR_ID, "2", "cup", "Bread"),
                scaledIngredient(SALT_ID, "0.5", "tsp", "Bread"),
            )

            val rows = ShoppingListCalculator.aggregateShoppingList(scaled, INGREDIENT_LOOKUP)

            assertThat(rows).hasSize(2)
            val flourRow = rows.find { it.ingredientId == FLOUR_ID }!!
            val saltRow = rows.find { it.ingredientId == SALT_ID }!!
            assertThat(flourRow.quantityRequired).isEqualByComparingTo(BigDecimal("5"))
            // 1.5 tsp bestFits to 0.5 tbsp (1.5/3 = 0.5)
            assertThat(saltRow.quantityRequired).isEqualByComparingTo(BigDecimal("0.5"))
            assertThat(saltRow.unit).isEqualTo("tbsp")
        }
    }

    @Nested
    inner class PurchaseStatusDerivation {

        @Test
        fun `done when purchased equals required`() {
            assertThat(PurchaseStatus.derive(BigDecimal("3"), BigDecimal("3"))).isEqualTo(PurchaseStatus.DONE)
        }

        @Test
        fun `done when purchased exceeds required`() {
            assertThat(PurchaseStatus.derive(BigDecimal("3"), BigDecimal("5"))).isEqualTo(PurchaseStatus.DONE)
        }

        @Test
        fun `more needed when partially purchased`() {
            assertThat(PurchaseStatus.derive(BigDecimal("5"), BigDecimal("2"))).isEqualTo(PurchaseStatus.MORE_NEEDED)
        }

        @Test
        fun `not purchased when zero purchased`() {
            assertThat(PurchaseStatus.derive(BigDecimal("5"), BigDecimal.ZERO)).isEqualTo(PurchaseStatus.NOT_PURCHASED)
        }

        @Test
        fun `no longer needed when required is zero but purchased positive`() {
            assertThat(PurchaseStatus.derive(BigDecimal.ZERO, BigDecimal("3"))).isEqualTo(PurchaseStatus.NO_LONGER_NEEDED)
        }

        @Test
        fun `no purchase record treated as zero purchased`() {
            val purchased: BigDecimal? = null
            assertThat(PurchaseStatus.derive(BigDecimal("5"), purchased ?: BigDecimal.ZERO)).isEqualTo(PurchaseStatus.NOT_PURCHASED)
        }

        @Test
        fun `purchased exactly equals required is done not more needed`() {
            assertThat(PurchaseStatus.derive(BigDecimal("2.5"), BigDecimal("2.5"))).isEqualTo(PurchaseStatus.DONE)
        }
    }
}
