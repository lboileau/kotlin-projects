package com.acme.services.camperservice.features.mealplan

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.fake.FakeIngredientClient
import com.acme.clients.ingredientclient.model.Ingredient
import com.acme.clients.mealplanclient.fake.FakeMealPlanClient
import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanDay
import com.acme.clients.mealplanclient.model.MealPlanRecipe
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
                unit = "g",
                quantityPurchased = BigDecimal("500"),
            ))

            // Update same ingredient+unit
            val result = service.updatePurchase(UpdatePurchaseParam(
                mealPlanId = mealPlan.id,
                userId = userId,
                ingredientId = ingredientId,
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
    }
}
