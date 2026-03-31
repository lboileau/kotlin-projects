package com.acme.services.camperservice.features.mealplan

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.fake.FakeIngredientClient
import com.acme.clients.ingredientclient.model.Ingredient
import com.acme.clients.mealplanclient.fake.FakeMealPlanClient
import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanDay
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import com.acme.clients.mealplanclient.model.ShoppingListManualItem
import com.acme.clients.mealplanclient.model.ShoppingListPurchase
import com.acme.clients.recipeclient.fake.FakeRecipeClient
import com.acme.clients.recipeclient.model.Recipe
import com.acme.clients.recipeclient.model.RecipeIngredient
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.*
import com.acme.services.camperservice.features.mealplan.service.MealPlanService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class MealPlanServiceTest {

    private val fakeMealPlanClient = FakeMealPlanClient()
    private val fakeRecipeClient = FakeRecipeClient()
    private val fakeIngredientClient = FakeIngredientClient()

    private val service = MealPlanService(fakeMealPlanClient, fakeRecipeClient, fakeIngredientClient)

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        fakeMealPlanClient.reset()
        fakeRecipeClient.reset()
        fakeIngredientClient.reset()
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private fun seedMealPlan(
        name: String = "Test Plan",
        servings: Int = 4,
        scalingMode: String = "fractional",
        isTemplate: Boolean = false,
        planId: UUID? = UUID.randomUUID(),
        sourceTemplateId: UUID? = null,
    ): MealPlan {
        val mealPlan = MealPlan(
            id = UUID.randomUUID(),
            planId = planId,
            name = name,
            servings = servings,
            scalingMode = scalingMode,
            isTemplate = isTemplate,
            sourceTemplateId = sourceTemplateId,
            createdBy = userId,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        fakeMealPlanClient.seed(mealPlan)
        return mealPlan
    }

    private fun seedDay(mealPlanId: UUID, dayNumber: Int): MealPlanDay {
        val day = MealPlanDay(
            id = UUID.randomUUID(),
            mealPlanId = mealPlanId,
            dayNumber = dayNumber,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        fakeMealPlanClient.seedDays(day)
        return day
    }

    private fun seedMealPlanRecipe(dayId: UUID, mealType: String, recipeId: UUID): MealPlanRecipe {
        val mpr = MealPlanRecipe(
            id = UUID.randomUUID(),
            mealPlanDayId = dayId,
            mealType = mealType,
            recipeId = recipeId,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        fakeMealPlanClient.seedRecipes(mpr)
        return mpr
    }

    private fun seedRecipe(
        name: String = "Test Recipe",
        baseServings: Int = 4,
    ): Recipe {
        val recipe = Recipe(
            id = UUID.randomUUID(),
            name = name,
            description = null,
            webLink = null,
            baseServings = baseServings,
            status = "published",
            createdBy = userId,
            duplicateOfId = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        fakeRecipeClient.seed(recipe)
        return recipe
    }

    private fun seedIngredient(
        name: String = "Tomato",
        category: String = "produce",
        defaultUnit: String = "g",
    ): Ingredient {
        val ingredient = Ingredient(
            id = UUID.randomUUID(),
            name = name,
            category = category,
            defaultUnit = defaultUnit,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        fakeIngredientClient.seed(ingredient)
        return ingredient
    }

    private fun seedRecipeIngredient(
        recipeId: UUID,
        ingredientId: UUID,
        quantity: BigDecimal,
        unit: String,
    ): RecipeIngredient {
        val ri = RecipeIngredient(
            id = UUID.randomUUID(),
            recipeId = recipeId,
            ingredientId = ingredientId,
            originalText = null,
            quantity = quantity,
            unit = unit,
            status = "approved",
            matchedIngredientId = null,
            suggestedIngredientName = null,
            reviewFlags = emptyList(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        fakeRecipeClient.seedIngredients(ri)
        return ri
    }

    private fun seedPurchase(
        mealPlanId: UUID,
        ingredientId: UUID,
        unit: String,
        quantityPurchased: BigDecimal,
    ): ShoppingListPurchase {
        val purchase = ShoppingListPurchase(
            id = UUID.randomUUID(),
            mealPlanId = mealPlanId,
            ingredientId = ingredientId,
            unit = unit,
            quantityPurchased = quantityPurchased,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        fakeMealPlanClient.seedPurchases(purchase)
        return purchase
    }

    private fun seedManualItem(
        mealPlanId: UUID,
        ingredientId: UUID? = null,
        description: String? = null,
        quantity: BigDecimal = BigDecimal.ONE,
        unit: String? = null,
        quantityPurchased: BigDecimal = BigDecimal.ZERO,
    ): ShoppingListManualItem {
        val item = ShoppingListManualItem(
            id = UUID.randomUUID(),
            mealPlanId = mealPlanId,
            ingredientId = ingredientId,
            description = description,
            quantity = quantity,
            unit = unit,
            quantityPurchased = quantityPurchased,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        fakeMealPlanClient.seedManualItems(item)
        return item
    }

    // ─── CreateMealPlan ─────────────────────────────────────────────────────

    @Nested
    inner class CreateMealPlan {

        @Test
        fun `create template meal plan succeeds`() {
            val result = service.create(CreateMealPlanParam(
                userId = userId,
                name = "Weekend Template",
                servings = 4,
                scalingMode = "fractional",
                isTemplate = true,
                planId = null,
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.name).isEqualTo("Weekend Template")
            assertThat(response.servings).isEqualTo(4)
            assertThat(response.scalingMode).isEqualTo("fractional")
            assertThat(response.isTemplate).isTrue()
            assertThat(response.planId).isNull()
        }

        @Test
        fun `create trip-bound meal plan succeeds`() {
            val planId = UUID.randomUUID()
            val result = service.create(CreateMealPlanParam(
                userId = userId,
                name = "Trip Meals",
                servings = 6,
                scalingMode = "round_up",
                isTemplate = false,
                planId = planId,
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.name).isEqualTo("Trip Meals")
            assertThat(response.servings).isEqualTo(6)
            assertThat(response.scalingMode).isEqualTo("round_up")
            assertThat(response.isTemplate).isFalse()
            assertThat(response.planId).isEqualTo(planId)
        }

        @Test
        fun `create with blank name returns Invalid`() {
            val result = service.create(CreateMealPlanParam(
                userId = userId,
                name = "  ",
                servings = 4,
                scalingMode = null,
                isTemplate = null,
                planId = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `create with zero servings returns Invalid`() {
            val result = service.create(CreateMealPlanParam(
                userId = userId,
                name = "Test",
                servings = 0,
                scalingMode = null,
                isTemplate = null,
                planId = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `create with negative servings returns Invalid`() {
            val result = service.create(CreateMealPlanParam(
                userId = userId,
                name = "Test",
                servings = -1,
                scalingMode = null,
                isTemplate = null,
                planId = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `create with invalid scaling mode returns Invalid`() {
            val result = service.create(CreateMealPlanParam(
                userId = userId,
                name = "Test",
                servings = 4,
                scalingMode = "invalid_mode",
                isTemplate = null,
                planId = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `create with null scaling mode defaults to fractional`() {
            val result = service.create(CreateMealPlanParam(
                userId = userId,
                name = "Default Scaling",
                servings = 4,
                scalingMode = null,
                isTemplate = null,
                planId = null,
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.scalingMode).isEqualTo("fractional")
        }

        @Test
        fun `create trip-bound when plan already has meal plan returns PlanAlreadyHasMealPlan`() {
            val planId = UUID.randomUUID()
            seedMealPlan(planId = planId)

            val result = service.create(CreateMealPlanParam(
                userId = userId,
                name = "Another Plan",
                servings = 4,
                scalingMode = null,
                isTemplate = null,
                planId = planId,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.PlanAlreadyHasMealPlan::class.java)
        }
    }

    // ─── GetMealPlanDetail ──────────────────────────────────────────────────

    @Nested
    inner class GetMealPlanDetail {

        @Test
        fun `get detail with days and recipes returns full detail`() {
            val mealPlan = seedMealPlan()
            val day1 = seedDay(mealPlan.id, 1)
            val recipe = seedRecipe("Pancakes")
            val ingredient = seedIngredient("Flour", "baking", "g")
            seedRecipeIngredient(recipe.id, ingredient.id, BigDecimal("200"), "g")
            seedMealPlanRecipe(day1.id, "breakfast", recipe.id)

            val result = service.getDetail(GetMealPlanDetailParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            assertThat(detail.id).isEqualTo(mealPlan.id)
            assertThat(detail.name).isEqualTo("Test Plan")
            assertThat(detail.days).hasSize(1)
            assertThat(detail.days[0].dayNumber).isEqualTo(1)
            assertThat(detail.days[0].meals.breakfast).hasSize(1)
            assertThat(detail.days[0].meals.breakfast[0].recipeName).isEqualTo("Pancakes")
            assertThat(detail.days[0].meals.breakfast[0].ingredients).hasSize(1)
            assertThat(detail.days[0].meals.breakfast[0].ingredients[0].ingredientName).isEqualTo("Flour")
        }

        @Test
        fun `get detail returns MealPlanNotFound when meal plan does not exist`() {
            val result = service.getDetail(GetMealPlanDetailParam(UUID.randomUUID(), userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }

        @Test
        fun `get detail of empty meal plan returns empty days list`() {
            val mealPlan = seedMealPlan()

            val result = service.getDetail(GetMealPlanDetailParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            assertThat(detail.days).isEmpty()
        }
    }

    // ─── GetMealPlanByPlanId ────────────────────────────────────────────────

    @Nested
    inner class GetMealPlanByPlanId {

        @Test
        fun `get by plan ID returns meal plan detail when found`() {
            val planId = UUID.randomUUID()
            val mealPlan = seedMealPlan(planId = planId)

            val result = service.getByPlanId(GetMealPlanByPlanIdParam(planId, userId))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            assertThat(detail).isNotNull()
            assertThat(detail!!.id).isEqualTo(mealPlan.id)
        }

        @Test
        fun `get by plan ID returns null when not found`() {
            val result = service.getByPlanId(GetMealPlanByPlanIdParam(UUID.randomUUID(), userId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).isNull()
        }
    }

    // ─── GetTemplates ───────────────────────────────────────────────────────

    @Nested
    inner class GetTemplates {

        @Test
        fun `get templates returns only templates`() {
            seedMealPlan(name = "Template 1", isTemplate = true, planId = null)
            seedMealPlan(name = "Template 2", isTemplate = true, planId = null)
            seedMealPlan(name = "Trip Plan", isTemplate = false, planId = UUID.randomUUID())

            val result = service.getTemplates(GetTemplatesParam(userId))

            assertThat(result.isSuccess).isTrue()
            val templates = (result as Result.Success).value
            assertThat(templates).hasSize(2)
            assertThat(templates.map { it.name }).containsExactlyInAnyOrder("Template 1", "Template 2")
            assertThat(templates.all { it.isTemplate }).isTrue()
        }

        @Test
        fun `get templates returns empty when no templates exist`() {
            seedMealPlan(name = "Trip Plan", isTemplate = false, planId = UUID.randomUUID())

            val result = service.getTemplates(GetTemplatesParam(userId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    // ─── UpdateMealPlan ─────────────────────────────────────────────────────

    @Nested
    inner class UpdateMealPlan {

        @Test
        fun `update name succeeds`() {
            val mealPlan = seedMealPlan()

            val result = service.update(UpdateMealPlanParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                name = "Updated Name",
                servings = null,
                scalingMode = null,
            ))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.name).isEqualTo("Updated Name")
        }

        @Test
        fun `update servings succeeds`() {
            val mealPlan = seedMealPlan(servings = 4)

            val result = service.update(UpdateMealPlanParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                name = null,
                servings = 8,
                scalingMode = null,
            ))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.servings).isEqualTo(8)
        }

        @Test
        fun `update scaling mode succeeds`() {
            val mealPlan = seedMealPlan(scalingMode = "fractional")

            val result = service.update(UpdateMealPlanParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                name = null,
                servings = null,
                scalingMode = "round_up",
            ))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.scalingMode).isEqualTo("round_up")
        }

        @Test
        fun `update returns MealPlanNotFound when meal plan does not exist`() {
            val result = service.update(UpdateMealPlanParam(
                mealPlanId = UUID.randomUUID(),
                userId = userId,
                name = "Nope",
                servings = null,
                scalingMode = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }

        @Test
        fun `update with blank name returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.update(UpdateMealPlanParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                name = "  ",
                servings = null,
                scalingMode = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `update with zero servings returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.update(UpdateMealPlanParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                name = null,
                servings = 0,
                scalingMode = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }
    }

    // ─── DeleteMealPlan ─────────────────────────────────────────────────────

    @Nested
    inner class DeleteMealPlan {

        @Test
        fun `delete succeeds`() {
            val mealPlan = seedMealPlan()

            val result = service.delete(DeleteMealPlanParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `delete returns MealPlanNotFound when meal plan does not exist`() {
            val result = service.delete(DeleteMealPlanParam(UUID.randomUUID(), userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }
    }

    // ─── CopyToTrip ─────────────────────────────────────────────────────────

    @Nested
    inner class CopyToTrip {

        @Test
        fun `copy template to trip succeeds with deep copy`() {
            val template = seedMealPlan(name = "Weekend Template", isTemplate = true, planId = null)
            val day1 = seedDay(template.id, 1)
            val recipe = seedRecipe("Eggs Benedict")
            seedMealPlanRecipe(day1.id, "breakfast", recipe.id)

            val targetPlanId = UUID.randomUUID()
            val result = service.copyToTrip(CopyToTripParam(
                mealPlanId = template.id,
                userId = userId,
                planId = targetPlanId,
                servings = 6,
            ))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            assertThat(detail.planId).isEqualTo(targetPlanId)
            assertThat(detail.isTemplate).isFalse()
            assertThat(detail.servings).isEqualTo(6)
            assertThat(detail.sourceTemplateId).isEqualTo(template.id)
            assertThat(detail.days).hasSize(1)
            assertThat(detail.days[0].dayNumber).isEqualTo(1)
            assertThat(detail.days[0].meals.breakfast).hasSize(1)
            assertThat(detail.days[0].meals.breakfast[0].recipeName).isEqualTo("Eggs Benedict")
        }

        @Test
        fun `copy non-template returns NotATemplate`() {
            val tripPlan = seedMealPlan(isTemplate = false)

            val result = service.copyToTrip(CopyToTripParam(
                mealPlanId = tripPlan.id,
                userId = userId,
                planId = UUID.randomUUID(),
                servings = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.NotATemplate::class.java)
        }

        @Test
        fun `copy to plan that already has meal plan returns PlanAlreadyHasMealPlan`() {
            val template = seedMealPlan(isTemplate = true, planId = null)
            val existingPlanId = UUID.randomUUID()
            seedMealPlan(planId = existingPlanId)

            val result = service.copyToTrip(CopyToTripParam(
                mealPlanId = template.id,
                userId = userId,
                planId = existingPlanId,
                servings = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.PlanAlreadyHasMealPlan::class.java)
        }

        @Test
        fun `copy template not found returns MealPlanNotFound`() {
            val result = service.copyToTrip(CopyToTripParam(
                mealPlanId = UUID.randomUUID(),
                userId = userId,
                planId = UUID.randomUUID(),
                servings = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }

        @Test
        fun `copy template uses source servings when servings param is null`() {
            val template = seedMealPlan(name = "Template", servings = 8, isTemplate = true, planId = null)

            val result = service.copyToTrip(CopyToTripParam(
                mealPlanId = template.id,
                userId = userId,
                planId = UUID.randomUUID(),
                servings = null,
            ))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.servings).isEqualTo(8)
        }

        @Test
        fun `copy to trip does not copy manual items`() {
            val template = seedMealPlan(name = "Template", isTemplate = true, planId = null)
            val ingredient = seedIngredient("Butter", "dairy", "g")
            seedManualItem(template.id, ingredientId = ingredient.id, quantity = BigDecimal("200"), unit = "g")
            seedManualItem(template.id, description = "Paper plates")

            val result = service.copyToTrip(CopyToTripParam(
                mealPlanId = template.id,
                userId = userId,
                planId = UUID.randomUUID(),
                servings = null,
            ))

            assertThat(result.isSuccess).isTrue()
            val tripMealPlan = (result as Result.Success).value

            // Verify the new trip meal plan has no manual items
            val shoppingList = (service.getShoppingList(GetShoppingListParam(tripMealPlan.id, userId)) as Result.Success).value
            assertThat(shoppingList.totalItems).isEqualTo(0)
        }
    }

    // ─── SaveAsTemplate ─────────────────────────────────────────────────────

    @Nested
    inner class SaveAsTemplate {

        @Test
        fun `save trip plan as template succeeds with deep copy`() {
            val tripPlan = seedMealPlan(name = "Trip Meals", isTemplate = false)
            val day1 = seedDay(tripPlan.id, 1)
            val recipe = seedRecipe("Campfire Stew")
            val mpr = seedMealPlanRecipe(day1.id, "dinner", recipe.id)
            // Seed a purchase that should NOT be copied
            seedPurchase(tripPlan.id, UUID.randomUUID(), "g", BigDecimal("500"))

            val result = service.saveAsTemplate(SaveAsTemplateParam(
                mealPlanId = tripPlan.id,
                userId = userId,
                name = "Saved Template",
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.isTemplate).isTrue()
            assertThat(response.name).isEqualTo("Saved Template")
            assertThat(response.id).isNotEqualTo(tripPlan.id)

            // Verify deep copy: template has its own days with new IDs
            val templateDays = (fakeMealPlanClient.getDays(
                com.acme.clients.mealplanclient.api.GetDaysParam(response.id)
            ) as Result.Success).value
            assertThat(templateDays).hasSize(1)
            assertThat(templateDays[0].id).isNotEqualTo(day1.id)
            assertThat(templateDays[0].dayNumber).isEqualTo(1)

            // Verify deep copy: template day has recipes with new IDs
            val templateRecipes = (fakeMealPlanClient.getRecipesByDayId(
                com.acme.clients.mealplanclient.api.GetRecipesByDayIdParam(templateDays[0].id)
            ) as Result.Success).value
            assertThat(templateRecipes).hasSize(1)
            assertThat(templateRecipes[0].id).isNotEqualTo(mpr.id)
            assertThat(templateRecipes[0].recipeId).isEqualTo(recipe.id)
            assertThat(templateRecipes[0].mealType).isEqualTo("dinner")

            // Verify purchases were NOT copied
            val templatePurchases = (fakeMealPlanClient.getPurchases(
                com.acme.clients.mealplanclient.api.GetPurchasesParam(response.id)
            ) as Result.Success).value
            assertThat(templatePurchases).isEmpty()
        }

        @Test
        fun `save template as template returns IsATemplate`() {
            val template = seedMealPlan(isTemplate = true, planId = null)

            val result = service.saveAsTemplate(SaveAsTemplateParam(
                mealPlanId = template.id,
                userId = userId,
                name = "Another Template",
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.IsATemplate::class.java)
        }

        @Test
        fun `save as template returns MealPlanNotFound when meal plan does not exist`() {
            val result = service.saveAsTemplate(SaveAsTemplateParam(
                mealPlanId = UUID.randomUUID(),
                userId = userId,
                name = "Template",
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }

        @Test
        fun `save as template with blank name returns Invalid`() {
            val tripPlan = seedMealPlan(isTemplate = false)

            val result = service.saveAsTemplate(SaveAsTemplateParam(
                mealPlanId = tripPlan.id,
                userId = userId,
                name = "  ",
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `save as template does not copy manual items`() {
            val tripPlan = seedMealPlan(isTemplate = false)
            val ingredient = seedIngredient("Salt", "spice", "g")
            seedManualItem(tripPlan.id, ingredientId = ingredient.id, quantity = BigDecimal("100"), unit = "g")
            seedManualItem(tripPlan.id, description = "Napkins")

            val result = service.saveAsTemplate(SaveAsTemplateParam(
                mealPlanId = tripPlan.id,
                userId = userId,
                name = "Template Copy",
            ))

            assertThat(result.isSuccess).isTrue()
            val template = (result as Result.Success).value

            // Verify the template has no manual items
            val shoppingList = (service.getShoppingList(GetShoppingListParam(template.id, userId)) as Result.Success).value
            assertThat(shoppingList.totalItems).isEqualTo(0)
        }
    }

    // ─── AddDay ─────────────────────────────────────────────────────────────

    @Nested
    inner class AddDay {

        @Test
        fun `add day succeeds`() {
            val mealPlan = seedMealPlan()

            val result = service.addDay(AddDayParam(mealPlan.id, userId, 1))

            assertThat(result.isSuccess).isTrue()
            val dayResponse = (result as Result.Success).value
            assertThat(dayResponse.dayNumber).isEqualTo(1)
            assertThat(dayResponse.meals.breakfast).isEmpty()
            assertThat(dayResponse.meals.lunch).isEmpty()
            assertThat(dayResponse.meals.dinner).isEmpty()
            assertThat(dayResponse.meals.snack).isEmpty()
        }

        @Test
        fun `add duplicate day number returns DuplicateDayNumber`() {
            val mealPlan = seedMealPlan()
            seedDay(mealPlan.id, 1)

            val result = service.addDay(AddDayParam(mealPlan.id, userId, 1))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.DuplicateDayNumber::class.java)
        }

        @Test
        fun `add day with zero day number returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.addDay(AddDayParam(mealPlan.id, userId, 0))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add day with negative day number returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.addDay(AddDayParam(mealPlan.id, userId, -1))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }
    }

    // ─── RemoveDay ──────────────────────────────────────────────────────────

    @Nested
    inner class RemoveDay {

        @Test
        fun `remove day succeeds`() {
            val mealPlan = seedMealPlan()
            val day = seedDay(mealPlan.id, 1)

            val result = service.removeDay(RemoveDayParam(mealPlan.id, day.id, userId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `remove non-existent day returns DayNotFound`() {
            val mealPlan = seedMealPlan()

            val result = service.removeDay(RemoveDayParam(mealPlan.id, UUID.randomUUID(), userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.DayNotFound::class.java)
        }
    }

    // ─── AddRecipeToMeal ────────────────────────────────────────────────────

    @Nested
    inner class AddRecipeToMeal {

        @Test
        fun `add recipe to meal succeeds`() {
            val mealPlan = seedMealPlan()
            val day = seedDay(mealPlan.id, 1)
            val recipe = seedRecipe("Pancakes", baseServings = 2)
            val ingredient = seedIngredient("Flour", "baking", "g")
            seedRecipeIngredient(recipe.id, ingredient.id, BigDecimal("200"), "g")

            val result = service.addRecipeToMeal(AddRecipeToMealParam(
                mealPlanId = mealPlan.id,
                dayId = day.id,
                userId = userId,
                mealType = "breakfast",
                recipeId = recipe.id,
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.recipeId).isEqualTo(recipe.id)
            assertThat(response.recipeName).isEqualTo("Pancakes")
            assertThat(response.baseServings).isEqualTo(2)
            assertThat(response.ingredients).hasSize(1)
            assertThat(response.ingredients[0].ingredientName).isEqualTo("Flour")
        }

        @Test
        fun `add non-existent recipe returns RecipeNotFound`() {
            val mealPlan = seedMealPlan()
            val day = seedDay(mealPlan.id, 1)

            val result = service.addRecipeToMeal(AddRecipeToMealParam(
                mealPlanId = mealPlan.id,
                dayId = day.id,
                userId = userId,
                mealType = "breakfast",
                recipeId = UUID.randomUUID(),
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.RecipeNotFound::class.java)
        }

        @Test
        fun `add recipe with invalid meal type returns Invalid`() {
            val mealPlan = seedMealPlan()
            val day = seedDay(mealPlan.id, 1)
            val recipe = seedRecipe()

            val result = service.addRecipeToMeal(AddRecipeToMealParam(
                mealPlanId = mealPlan.id,
                dayId = day.id,
                userId = userId,
                mealType = "brunch",
                recipeId = recipe.id,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }
    }

    // ─── RemoveRecipeFromMeal ───────────────────────────────────────────────

    @Nested
    inner class RemoveRecipeFromMeal {

        @Test
        fun `remove recipe from meal succeeds`() {
            val mealPlan = seedMealPlan()
            val day = seedDay(mealPlan.id, 1)
            val recipe = seedRecipe()
            val mpr = seedMealPlanRecipe(day.id, "dinner", recipe.id)

            val result = service.removeRecipeFromMeal(RemoveRecipeFromMealParam(mpr.id, userId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `remove non-existent recipe returns RecipeNotFound`() {
            val result = service.removeRecipeFromMeal(RemoveRecipeFromMealParam(UUID.randomUUID(), userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.RecipeNotFound::class.java)
        }
    }

    // ─── GetShoppingList ────────────────────────────────────────────────────

    @Nested
    inner class GetShoppingList {

        @Test
        fun `empty meal plan returns empty shopping list`() {
            val mealPlan = seedMealPlan()

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.mealPlanId).isEqualTo(mealPlan.id)
            assertThat(response.totalItems).isEqualTo(0)
            assertThat(response.categories).isEmpty()
        }

        @Test
        fun `shopping list scales ingredients and groups by category`() {
            // Meal plan: 8 servings, fractional scaling
            val mealPlan = seedMealPlan(servings = 8, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)

            // Recipe with base 4 servings → scale factor 2x
            val recipe = seedRecipe("Pasta", baseServings = 4)
            val tomato = seedIngredient("Tomato", "produce", "g")
            val pasta = seedIngredient("Pasta", "dry goods", "g")
            seedRecipeIngredient(recipe.id, tomato.id, BigDecimal("200"), "g")
            seedRecipeIngredient(recipe.id, pasta.id, BigDecimal("400"), "g")
            seedMealPlanRecipe(day1.id, "dinner", recipe.id)

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.totalItems).isEqualTo(2)
            assertThat(response.servings).isEqualTo(8)
            assertThat(response.scalingMode).isEqualTo("fractional")

            val allItems = response.categories.flatMap { it.items }
            val tomatoItem = allItems.find { it.ingredientName == "Tomato" }!!
            assertThat(tomatoItem.quantityRequired.compareTo(BigDecimal("400"))).isEqualTo(0)
            assertThat(tomatoItem.unit).isEqualTo("g")
            assertThat(tomatoItem.status).isEqualTo("not_purchased")
            assertThat(tomatoItem.usedInRecipes).containsExactly("Pasta")

            val pastaItem = allItems.find { it.ingredientName == "Pasta" }!!
            assertThat(pastaItem.quantityRequired.compareTo(BigDecimal("800"))).isEqualTo(0)
        }

        @Test
        fun `multiple recipes same ingredient are aggregated`() {
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)

            val tomato = seedIngredient("Tomato", "produce", "g")

            // Recipe 1: 200g tomato (base 4)
            val recipe1 = seedRecipe("Salad", baseServings = 4)
            seedRecipeIngredient(recipe1.id, tomato.id, BigDecimal("200"), "g")
            seedMealPlanRecipe(day1.id, "lunch", recipe1.id)

            // Recipe 2: 300g tomato (base 4)
            val recipe2 = seedRecipe("Sauce", baseServings = 4)
            seedRecipeIngredient(recipe2.id, tomato.id, BigDecimal("300"), "g")
            seedMealPlanRecipe(day1.id, "dinner", recipe2.id)

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            val tomatoItem = allItems.find { it.ingredientName == "Tomato" }!!
            // 200 + 300 = 500g → bestFit converts to 0.5 kg
            assertThat(tomatoItem.quantityRequired.compareTo(BigDecimal("0.5"))).isEqualTo(0)
            assertThat(tomatoItem.unit).isEqualTo("kg")
            assertThat(tomatoItem.usedInRecipes).containsExactly("Salad", "Sauce")
        }

        @Test
        fun `purchase status is done when fully purchased`() {
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)

            val tomato = seedIngredient("Tomato", "produce", "g")
            val recipe = seedRecipe("Salad", baseServings = 4)
            seedRecipeIngredient(recipe.id, tomato.id, BigDecimal("200"), "g")
            seedMealPlanRecipe(day1.id, "lunch", recipe.id)

            // Purchase exactly 200g
            seedPurchase(mealPlan.id, tomato.id, "g", BigDecimal("200"))

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            val allItems = response.categories.flatMap { it.items }
            val tomatoItem = allItems.find { it.ingredientName == "Tomato" }!!
            assertThat(tomatoItem.status).isEqualTo("done")
            assertThat(tomatoItem.quantityPurchased.compareTo(BigDecimal("200"))).isEqualTo(0)
            assertThat(response.fullyPurchasedCount).isEqualTo(1)
        }

        @Test
        fun `purchase status is more_needed when partially purchased`() {
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)

            val tomato = seedIngredient("Tomato", "produce", "g")
            val recipe = seedRecipe("Salad", baseServings = 4)
            seedRecipeIngredient(recipe.id, tomato.id, BigDecimal("200"), "g")
            seedMealPlanRecipe(day1.id, "lunch", recipe.id)

            // Purchase only 100g of 200g needed
            seedPurchase(mealPlan.id, tomato.id, "g", BigDecimal("100"))

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            val tomatoItem = allItems.find { it.ingredientName == "Tomato" }!!
            assertThat(tomatoItem.status).isEqualTo("more_needed")
        }

        @Test
        fun `purchase status is not_purchased when nothing purchased`() {
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)

            val tomato = seedIngredient("Tomato", "produce", "g")
            val recipe = seedRecipe("Salad", baseServings = 4)
            seedRecipeIngredient(recipe.id, tomato.id, BigDecimal("200"), "g")
            seedMealPlanRecipe(day1.id, "lunch", recipe.id)

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            val tomatoItem = allItems.find { it.ingredientName == "Tomato" }!!
            assertThat(tomatoItem.status).isEqualTo("not_purchased")
        }

        @Test
        fun `orphaned purchase shows as no_longer_needed`() {
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            // No recipes, but there's a leftover purchase
            val tomato = seedIngredient("Tomato", "produce", "g")
            seedPurchase(mealPlan.id, tomato.id, "g", BigDecimal("500"))

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            val tomatoItem = allItems.find { it.ingredientName == "Tomato" }!!
            assertThat(tomatoItem.status).isEqualTo("no_longer_needed")
            assertThat(tomatoItem.quantityRequired.compareTo(BigDecimal.ZERO)).isEqualTo(0)
            assertThat(tomatoItem.quantityPurchased.compareTo(BigDecimal("500"))).isEqualTo(0)
        }

        @Test
        fun `purchase in convertible unit is converted when bestFit changes shopping list unit`() {
            // Purchase recorded in grams, but bestFit converts the shopping list row to kg
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)

            val tomato = seedIngredient("Tomato", "produce", "g")
            val recipe1 = seedRecipe("Salad", baseServings = 4)
            seedRecipeIngredient(recipe1.id, tomato.id, BigDecimal("200"), "g")
            seedMealPlanRecipe(day1.id, "lunch", recipe1.id)

            val recipe2 = seedRecipe("Sauce", baseServings = 4)
            seedRecipeIngredient(recipe2.id, tomato.id, BigDecimal("300"), "g")
            seedMealPlanRecipe(day1.id, "dinner", recipe2.id)

            // Purchase 200g — the shopping list row will be in kg (0.5 kg) due to bestFit
            seedPurchase(mealPlan.id, tomato.id, "g", BigDecimal("200"))

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            val tomatoItem = allItems.find { it.ingredientName == "Tomato" }!!
            // Row is 0.5 kg, purchase of 200g = 0.2 kg → status should be more_needed
            assertThat(tomatoItem.unit).isEqualTo("kg")
            assertThat(tomatoItem.quantityRequired.compareTo(BigDecimal("0.5"))).isEqualTo(0)
            assertThat(tomatoItem.quantityPurchased.compareTo(BigDecimal("0.2"))).isEqualTo(0)
            assertThat(tomatoItem.status).isEqualTo("more_needed")
        }

        @Test
        fun `purchase in convertible unit does not show as no_longer_needed`() {
            // When bestFit changes the unit, the old purchase should NOT appear as orphaned
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)

            val tomato = seedIngredient("Tomato", "produce", "g")
            val recipe1 = seedRecipe("Salad", baseServings = 4)
            seedRecipeIngredient(recipe1.id, tomato.id, BigDecimal("200"), "g")
            seedMealPlanRecipe(day1.id, "lunch", recipe1.id)

            val recipe2 = seedRecipe("Sauce", baseServings = 4)
            seedRecipeIngredient(recipe2.id, tomato.id, BigDecimal("300"), "g")
            seedMealPlanRecipe(day1.id, "dinner", recipe2.id)

            // Purchase 500g — the shopping list row will be in kg due to bestFit
            seedPurchase(mealPlan.id, tomato.id, "g", BigDecimal("500"))

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            // Should be exactly one item (kg row with converted purchase), no orphaned "no_longer_needed"
            val tomatoItems = allItems.filter { it.ingredientName == "Tomato" }
            assertThat(tomatoItems).hasSize(1)
            assertThat(tomatoItems[0].unit).isEqualTo("kg")
            assertThat(tomatoItems[0].status).isEqualTo("done")
            assertThat(tomatoItems[0].quantityPurchased.compareTo(BigDecimal("0.5"))).isEqualTo(0)
        }

        @Test
        fun `zero quantity orphaned purchases are filtered out`() {
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            // No recipes, but there's a zero-quantity leftover purchase
            val tomato = seedIngredient("Tomato", "produce", "g")
            seedPurchase(mealPlan.id, tomato.id, "g", BigDecimal.ZERO)

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            // Zero-quantity orphan should be filtered out entirely
            assertThat(allItems.filter { it.ingredientName == "Tomato" }).isEmpty()
        }

        @Test
        fun `shopping list returns MealPlanNotFound when meal plan does not exist`() {
            val result = service.getShoppingList(GetShoppingListParam(UUID.randomUUID(), userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }

        @Test
        fun `shopping list with round_up scaling mode uses ceiling`() {
            // 6 servings / 4 base = 1.5 → round_up → 2
            val mealPlan = seedMealPlan(servings = 6, scalingMode = "round_up")
            val day1 = seedDay(mealPlan.id, 1)

            val flour = seedIngredient("Flour", "baking", "g")
            val recipe = seedRecipe("Bread", baseServings = 4)
            seedRecipeIngredient(recipe.id, flour.id, BigDecimal("500"), "g")
            seedMealPlanRecipe(day1.id, "dinner", recipe.id)

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            val flourItem = allItems.find { it.ingredientName == "Flour" }!!
            // 500 * 2 = 1000g → bestFit converts to 1 kg (round_up: ceil(6/4) = 2)
            assertThat(flourItem.quantityRequired.compareTo(BigDecimal("1"))).isEqualTo(0)
            assertThat(flourItem.unit).isEqualTo("kg")
        }

        @Test
        fun `shopping list only includes approved ingredients`() {
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)

            val approvedIngredient = seedIngredient("Flour", "baking", "g")
            val pendingIngredient = seedIngredient("Mystery Spice", "spices", "tsp")

            val recipe = seedRecipe("Bread", baseServings = 4)
            seedRecipeIngredient(recipe.id, approvedIngredient.id, BigDecimal("500"), "g")

            // Add a pending_review ingredient directly
            val pendingRI = RecipeIngredient(
                id = UUID.randomUUID(),
                recipeId = recipe.id,
                ingredientId = pendingIngredient.id,
                originalText = "some spice",
                quantity = BigDecimal("2"),
                unit = "tsp",
                status = "pending_review",
                matchedIngredientId = null,
                suggestedIngredientName = "Mystery Spice",
                reviewFlags = listOf("NEW_INGREDIENT"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            fakeRecipeClient.seedIngredients(pendingRI)

            seedMealPlanRecipe(day1.id, "dinner", recipe.id)

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            assertThat(allItems).hasSize(1)
            assertThat(allItems[0].ingredientName).isEqualTo("Flour")
        }
    }

    // ─── UpdatePurchase ─────────────────────────────────────────────────────

    @Nested
    inner class UpdatePurchase {

        @Test
        fun `create new purchase succeeds`() {
            val mealPlan = seedMealPlan()
            val ingredientId = UUID.randomUUID()

            val result = service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredientId,
                manualItemId = null,
                unit = "g",
                quantityPurchased = BigDecimal("500"),
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.mealPlanId).isEqualTo(mealPlan.id)
            assertThat(response.ingredientId).isEqualTo(ingredientId)
            assertThat(response.unit).isEqualTo("g")
            assertThat(response.quantityPurchased.compareTo(BigDecimal("500"))).isEqualTo(0)
        }

        @Test
        fun `upsert existing purchase updates quantity`() {
            val mealPlan = seedMealPlan()
            val ingredientId = UUID.randomUUID()

            // First purchase
            service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredientId,
                manualItemId = null,
                unit = "g",
                quantityPurchased = BigDecimal("500"),
            ))

            // Update same ingredient+unit
            val result = service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredientId,
                manualItemId = null,
                unit = "g",
                quantityPurchased = BigDecimal("750"),
            ))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.quantityPurchased.compareTo(BigDecimal("750"))).isEqualTo(0)
        }

        @Test
        fun `update purchase returns MealPlanNotFound when meal plan does not exist`() {
            val result = service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = UUID.randomUUID(),
                userId = userId,
                ingredientId = UUID.randomUUID(),
                manualItemId = null,
                unit = "g",
                quantityPurchased = BigDecimal("500"),
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }

        @Test
        fun `update purchase with negative quantity returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = UUID.randomUUID(),
                manualItemId = null,
                unit = "g",
                quantityPurchased = BigDecimal("-1"),
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }
    }

    // ─── ResetPurchases ─────────────────────────────────────────────────────

    @Nested
    inner class ResetPurchases {

        @Test
        fun `reset purchases succeeds`() {
            val mealPlan = seedMealPlan()
            seedPurchase(mealPlan.id, UUID.randomUUID(), "g", BigDecimal("500"))
            seedPurchase(mealPlan.id, UUID.randomUUID(), "kg", BigDecimal("2"))

            val result = service.resetPurchases(ResetPurchasesParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `reset purchases returns MealPlanNotFound when meal plan does not exist`() {
            val result = service.resetPurchases(ResetPurchasesParam(UUID.randomUUID(), userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }

        @Test
        fun `reset purchases also resets manual item quantities`() {
            val mealPlan = seedMealPlan()
            seedPurchase(mealPlan.id, UUID.randomUUID(), "g", BigDecimal("500"))
            val ingredient = seedIngredient("Butter", "dairy", "g")
            seedManualItem(mealPlan.id, ingredientId = ingredient.id, quantity = BigDecimal("200"), unit = "g", quantityPurchased = BigDecimal("100"))
            seedManualItem(mealPlan.id, description = "Paper plates", quantityPurchased = BigDecimal("1"))

            val result = service.resetPurchases(ResetPurchasesParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()

            // Verify manual items were reset
            val shoppingList = (service.getShoppingList(GetShoppingListParam(mealPlan.id, userId)) as Result.Success).value
            val manualItems = shoppingList.categories.flatMap { it.items }.filter { it.source == "manual" }
            assertThat(manualItems).hasSize(2)
            assertThat(manualItems.all { it.quantityPurchased.compareTo(BigDecimal.ZERO) == 0 }).isTrue()
        }
    }

    // ─── AddManualItem ──────────────────────────────────────────────────────

    @Nested
    inner class AddManualItem {

        @Test
        fun `add ingredient-based manual item returns correct response`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient("Butter", "dairy", "g")

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = null,
                quantity = BigDecimal("250"),
                unit = "g",
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.id).isNotNull()
            assertThat(response.ingredientId).isEqualTo(ingredient.id)
            assertThat(response.ingredientName).isEqualTo("Butter")
            assertThat(response.description).isNull()
            assertThat(response.quantity).isEqualByComparingTo(BigDecimal("250"))
            assertThat(response.unit).isEqualTo("g")
            assertThat(response.quantityPurchased).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(response.status).isEqualTo("not_purchased")
            assertThat(response.category).isEqualTo("dairy")
        }

        @Test
        fun `add free-form manual item returns correct response`() {
            val mealPlan = seedMealPlan()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = null,
                description = "Paper plates",
                quantity = null,
                unit = null,
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.ingredientId).isNull()
            assertThat(response.ingredientName).isNull()
            assertThat(response.description).isEqualTo("Paper plates")
            assertThat(response.quantity).isEqualByComparingTo(BigDecimal.ONE)
            assertThat(response.unit).isNull()
            assertThat(response.status).isEqualTo("not_purchased")
            assertThat(response.category).isEqualTo("misc")
        }

        @Test
        fun `add manual item with non-existent ingredient returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = UUID.randomUUID(),
                description = null,
                quantity = BigDecimal("100"),
                unit = "g",
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add manual item with non-existent meal plan returns MealPlanNotFound`() {
            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = UUID.randomUUID(),
                userId = userId,
                ingredientId = null,
                description = "Test",
                quantity = null,
                unit = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }

        @Test
        fun `add duplicate ingredient and unit returns DuplicateManualItem`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient("Butter", "dairy", "g")

            service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = null,
                quantity = BigDecimal("100"),
                unit = "g",
            ))

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = null,
                quantity = BigDecimal("200"),
                unit = "g",
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.DuplicateManualItem::class.java)
        }

        @Test
        fun `add with both ingredientId and description returns Invalid`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = "Some text",
                quantity = BigDecimal("100"),
                unit = "g",
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add with neither ingredientId nor description returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = null,
                description = null,
                quantity = null,
                unit = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add free-form with quantity provided returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = null,
                description = "Paper plates",
                quantity = BigDecimal("5"),
                unit = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add free-form with unit provided returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = null,
                description = "Paper plates",
                quantity = null,
                unit = "g",
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add ingredient-based with null quantity returns Invalid`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = null,
                quantity = null,
                unit = "g",
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add ingredient-based with null unit returns Invalid`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = null,
                quantity = BigDecimal("100"),
                unit = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add ingredient-based with zero quantity returns Invalid`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = null,
                quantity = BigDecimal.ZERO,
                unit = "g",
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add ingredient-based with invalid unit returns Invalid`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = null,
                quantity = BigDecimal("100"),
                unit = "invalid_unit",
            ))

            // Note: service-level validation delegates to client which validates units
            // If service doesn't validate unit, this will succeed but client will reject
            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add free-form with blank description returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = null,
                description = "   ",
                quantity = null,
                unit = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add free-form with description exceeding 500 chars returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = null,
                description = "a".repeat(501),
                quantity = null,
                unit = null,
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `add same ingredient with different unit succeeds`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient("Flour", "baking", "g")

            val r1 = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = null,
                quantity = BigDecimal("200"),
                unit = "g",
            ))
            val r2 = service.addManualItem(AddManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredient.id,
                description = null,
                quantity = BigDecimal("2"),
                unit = "cup",
            ))

            assertThat(r1.isSuccess).isTrue()
            assertThat(r2.isSuccess).isTrue()
            assertThat((r1 as Result.Success).value.id).isNotEqualTo((r2 as Result.Success).value.id)
        }
    }

    // ─── RemoveManualItem ───────────────────────────────────────────────────

    @Nested
    inner class RemoveManualItem {

        @Test
        fun `remove manual item succeeds`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient("Butter", "dairy", "g")
            val item = seedManualItem(mealPlan.id, ingredientId = ingredient.id, quantity = BigDecimal("200"), unit = "g")

            val result = service.removeManualItem(RemoveManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                itemId = item.id,
            ))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `remove non-existent manual item returns ManualItemNotFound`() {
            val mealPlan = seedMealPlan()

            val result = service.removeManualItem(RemoveManualItemParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                itemId = UUID.randomUUID(),
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.ManualItemNotFound::class.java)
        }

        @Test
        fun `remove manual item with non-existent meal plan returns MealPlanNotFound`() {
            val result = service.removeManualItem(RemoveManualItemParam(
                mealPlanId = UUID.randomUUID(),
                userId = userId,
                itemId = UUID.randomUUID(),
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.MealPlanNotFound::class.java)
        }
    }

    // ─── GetShoppingList with Manual Items ──────────────────────────────────

    @Nested
    inner class GetShoppingListWithManualItems {

        @Test
        fun `shopping list includes manual items with source manual`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient("Butter", "dairy", "g")
            seedManualItem(mealPlan.id, ingredientId = ingredient.id, quantity = BigDecimal("200"), unit = "g")

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            val allItems = response.categories.flatMap { it.items }
            assertThat(allItems).hasSize(1)
            val manualItem = allItems[0]
            assertThat(manualItem.source).isEqualTo("manual")
            assertThat(manualItem.manualItemId).isNotNull()
            assertThat(manualItem.ingredientName).isEqualTo("Butter")
            assertThat(manualItem.quantityRequired).isEqualByComparingTo(BigDecimal("200"))
            assertThat(manualItem.unit).isEqualTo("g")
            assertThat(manualItem.usedInRecipes).isEmpty()
        }

        @Test
        fun `free-form manual items appear in misc category`() {
            val mealPlan = seedMealPlan()
            seedManualItem(mealPlan.id, description = "Paper plates")

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            val miscCategory = response.categories.find { it.category == "misc" }
            assertThat(miscCategory).isNotNull
            assertThat(miscCategory!!.items).hasSize(1)
            assertThat(miscCategory.items[0].description).isEqualTo("Paper plates")
            assertThat(miscCategory.items[0].ingredientName).isNull()
            assertThat(miscCategory.items[0].source).isEqualTo("manual")
        }

        @Test
        fun `ingredient-based manual items use ingredient category`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient("Olive Oil", "pantry", "ml")
            seedManualItem(mealPlan.id, ingredientId = ingredient.id, quantity = BigDecimal("500"), unit = "ml")

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            val pantryCategory = response.categories.find { it.category == "pantry" }
            assertThat(pantryCategory).isNotNull
            assertThat(pantryCategory!!.items).hasSize(1)
            assertThat(pantryCategory.items[0].ingredientName).isEqualTo("Olive Oil")
        }

        @Test
        fun `recipe items have source recipe and null manualItemId`() {
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)
            val tomato = seedIngredient("Tomato", "produce", "g")
            val recipe = seedRecipe("Salad", baseServings = 4)
            seedRecipeIngredient(recipe.id, tomato.id, BigDecimal("200"), "g")
            seedMealPlanRecipe(day1.id, "lunch", recipe.id)

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val allItems = (result as Result.Success).value.categories.flatMap { it.items }
            val recipeItem = allItems.find { it.ingredientName == "Tomato" }!!
            assertThat(recipeItem.source).isEqualTo("recipe")
            assertThat(recipeItem.manualItemId).isNull()
            assertThat(recipeItem.description).isNull()
        }

        @Test
        fun `shopping list with both recipe and manual items counts all`() {
            val mealPlan = seedMealPlan(servings = 4, scalingMode = "fractional")
            val day1 = seedDay(mealPlan.id, 1)
            val tomato = seedIngredient("Tomato", "produce", "g")
            val recipe = seedRecipe("Salad", baseServings = 4)
            seedRecipeIngredient(recipe.id, tomato.id, BigDecimal("200"), "g")
            seedMealPlanRecipe(day1.id, "lunch", recipe.id)

            val butter = seedIngredient("Butter", "dairy", "g")
            seedManualItem(mealPlan.id, ingredientId = butter.id, quantity = BigDecimal("200"), unit = "g")
            seedManualItem(mealPlan.id, description = "Napkins")

            val result = service.getShoppingList(GetShoppingListParam(mealPlan.id, userId))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.totalItems).isEqualTo(3)
        }
    }

    // ─── UpdatePurchase for Manual Items ────────────────────────────────────

    @Nested
    inner class UpdatePurchaseManualItem {

        @Test
        fun `update purchase on manual item via manualItemId succeeds`() {
            val mealPlan = seedMealPlan()
            val ingredient = seedIngredient("Butter", "dairy", "g")
            val item = seedManualItem(mealPlan.id, ingredientId = ingredient.id, quantity = BigDecimal("200"), unit = "g")

            val result = service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = null,
                manualItemId = item.id,
                unit = null,
                quantityPurchased = BigDecimal("150"),
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.quantityPurchased).isEqualByComparingTo(BigDecimal("150"))
        }

        @Test
        fun `update purchase on non-existent manual item returns ManualItemNotFound`() {
            val mealPlan = seedMealPlan()

            val result = service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = null,
                manualItemId = UUID.randomUUID(),
                unit = null,
                quantityPurchased = BigDecimal("10"),
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.ManualItemNotFound::class.java)
        }

        @Test
        fun `update purchase with both ingredientId and manualItemId returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = UUID.randomUUID(),
                manualItemId = UUID.randomUUID(),
                unit = "g",
                quantityPurchased = BigDecimal("10"),
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }

        @Test
        fun `update purchase with neither ingredientId nor manualItemId returns Invalid`() {
            val mealPlan = seedMealPlan()

            val result = service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = null,
                manualItemId = null,
                unit = null,
                quantityPurchased = BigDecimal("10"),
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(MealPlanError.Invalid::class.java)
        }
    }
}
