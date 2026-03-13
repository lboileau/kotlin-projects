# Meal Plan Client — AI Context

## Overview
JDBI data access client for meal plans, meal plan days, meal plan recipes, and shopping list purchases.

## Package
`com.acme.clients.mealplanclient`

## Public API

### MealPlanClient (interface)
- **Meal Plans**: create, getById, getByPlanId, getTemplates, update, delete
- **Days**: addDay, getDays, removeDay
- **Recipes**: addRecipe, getRecipesByDayId, getRecipesByMealPlanId, removeRecipe
- **Shopping List Purchases**: getPurchases, upsertPurchase, deletePurchases

All operations return `Result<T, AppError>`.

### Models
- `MealPlan` — meal plan container (template or trip-bound)
- `MealPlanDay` — numbered day within a meal plan
- `MealPlanRecipe` — recipe reference placed on a day/meal
- `ShoppingListPurchase` — purchase tracking per ingredient per unit per meal plan

### Factory
- `createMealPlanClient()` — creates a JDBI-backed client using DB env vars

### Fake (testFixtures)
- `FakeMealPlanClient` — in-memory fake for unit testing

## Gradle Module
`:clients:meal-plan-client`
