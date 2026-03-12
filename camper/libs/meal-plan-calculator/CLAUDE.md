# Meal Plan Calculator Library — AI Context

## Overview
Pure-logic library for unit conversion and shopping list computation. No I/O, no Spring dependencies.

## Package
`com.acme.libs.mealplancalculator`

## Public API

### UnitConverter (object)
- `convert(quantity, sourceUnit, targetUnit)`: Convert between compatible units. Returns null if incompatible.
- `areCompatible(unitA, unitB)`: Check if two units can be converted between each other.
- `bestFit(quantity, unit)`: Find the best display unit (e.g., 48 tsp -> 1 cup).
- `categoryOf(unit)`: Get the UnitCategory (VOLUME, WEIGHT, COUNT) for a unit string.

### ShoppingListCalculator (object)
- `scaleIngredients(recipeIngredients, servings, scalingMode)`: Scale recipe ingredients by target servings.
- `aggregateShoppingList(scaledIngredients, ingredientLookup)`: Group and sum scaled ingredients into shopping list rows.

### Model Types
- `ScalingMode` — FRACTIONAL or ROUND_UP
- `UnitCategory` — VOLUME, WEIGHT, COUNT
- `RecipeIngredientWithMeta` — recipe ingredient with recipe name, base servings, ingredient info
- `IngredientInfo` — ingredient ID, name, category (for lookup during aggregation)
- `ScaledIngredient` — ingredient after scaling (quantity adjusted for servings)
- `ShoppingListRow` — aggregated shopping list entry with required quantity, unit, and source recipes
- `PurchaseStatus` — DONE, MORE_NEEDED, NOT_PURCHASED, NO_LONGER_NEEDED

## Unit Categories
- **Volume**: tsp, tbsp, cup, ml, l (all convertible)
- **Weight**: g, kg, oz, lb (all convertible)
- **Count**: pieces, whole, bunch, can, clove, pinch, slice, sprig (each is its own type, not convertible)

## Gradle Module
`:libs:meal-plan-calculator`
