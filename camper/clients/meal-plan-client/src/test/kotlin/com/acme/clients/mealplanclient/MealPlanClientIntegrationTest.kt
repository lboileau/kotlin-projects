package com.acme.clients.mealplanclient

import com.acme.clients.common.Result
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.error.ValidationError
import com.acme.clients.mealplanclient.api.AddDayParam
import com.acme.clients.mealplanclient.api.AddManualItemParam
import com.acme.clients.mealplanclient.api.AddRecipeParam
import com.acme.clients.mealplanclient.api.CreateMealPlanParam
import com.acme.clients.mealplanclient.api.DeleteMealPlanParam
import com.acme.clients.mealplanclient.api.DeletePurchasesParam
import com.acme.clients.mealplanclient.api.GetByIdParam
import com.acme.clients.mealplanclient.api.GetByPlanIdParam
import com.acme.clients.mealplanclient.api.GetDaysParam
import com.acme.clients.mealplanclient.api.GetManualItemsParam
import com.acme.clients.mealplanclient.api.GetPurchasesParam
import com.acme.clients.mealplanclient.api.GetRecipesByDayIdParam
import com.acme.clients.mealplanclient.api.GetRecipesByMealPlanIdParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.mealplanclient.api.RemoveDayParam
import com.acme.clients.mealplanclient.api.RemoveManualItemParam
import com.acme.clients.mealplanclient.api.RemoveRecipeParam
import com.acme.clients.mealplanclient.api.ResetManualItemPurchasesParam
import com.acme.clients.mealplanclient.api.UpdateManualItemPurchaseParam
import com.acme.clients.mealplanclient.api.UpdateMealPlanParam
import com.acme.clients.mealplanclient.api.UpsertPurchaseParam
import com.acme.clients.mealplanclient.test.MealPlanTestDb
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.util.UUID

@Testcontainers
class MealPlanClientIntegrationTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("camper_db")
            .withUsername("postgres")
            .withPassword("postgres")

        private lateinit var client: MealPlanClient
        private lateinit var jdbi: org.jdbi.v3.core.Jdbi

        @BeforeAll
        @JvmStatic
        fun setup() {
            MealPlanTestDb.cleanAndMigrate(postgres.jdbcUrl, postgres.username, postgres.password)

            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            client = createMealPlanClient()
            jdbi = org.jdbi.v3.core.Jdbi.create(postgres.jdbcUrl, postgres.username, postgres.password)
        }
    }

    private lateinit var testUserId: UUID
    private lateinit var testPlanId: UUID
    private lateinit var testRecipeId: UUID
    private lateinit var testIngredientId: UUID

    @BeforeEach
    fun truncateAndSeed() {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                """
                TRUNCATE TABLE shopping_list_manual_items, shopping_list_purchases, meal_plan_recipes, meal_plan_days, meal_plans,
                    recipe_ingredients, recipes, ingredients, items, assignment_members, assignments,
                    itinerary_events, itineraries, invitations, plan_members, plans, users CASCADE
                """.trimIndent()
            ).execute()
        }
        testUserId = UUID.randomUUID()
        testPlanId = UUID.randomUUID()
        testRecipeId = UUID.randomUUID()
        testIngredientId = UUID.randomUUID()
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate("INSERT INTO users (id, email, username) VALUES (:id, :email, :username)")
                .bind("id", testUserId).bind("email", "test@example.com").bind("username", "testuser").execute()
            handle.createUpdate("INSERT INTO plans (id, name, visibility, owner_id) VALUES (:id, :name, :visibility, :ownerId)")
                .bind("id", testPlanId).bind("name", "Test Plan").bind("visibility", "private").bind("ownerId", testUserId).execute()
            handle.createUpdate("INSERT INTO recipes (id, name, base_servings, status, created_by) VALUES (:id, :name, :baseServings, :status, :createdBy)")
                .bind("id", testRecipeId).bind("name", "Test Recipe").bind("baseServings", 4).bind("status", "published").bind("createdBy", testUserId).execute()
            handle.createUpdate("INSERT INTO ingredients (id, name, category, default_unit) VALUES (:id, :name, :category, :defaultUnit)")
                .bind("id", testIngredientId).bind("name", "Salt").bind("category", "spice").bind("defaultUnit", "g").execute()
        }
    }

    private fun createTemplate(name: String = "Template", servings: Int = 4): com.acme.clients.mealplanclient.model.MealPlan {
        val result = client.create(
            CreateMealPlanParam(
                planId = null, name = name, servings = servings, scalingMode = "fractional",
                isTemplate = true, sourceTemplateId = null, createdBy = testUserId
            )
        )
        return (result as Result.Success).value
    }

    private fun createTripMealPlan(planId: UUID = testPlanId, name: String = "Trip Meals", servings: Int = 6): com.acme.clients.mealplanclient.model.MealPlan {
        val result = client.create(
            CreateMealPlanParam(
                planId = planId, name = name, servings = servings, scalingMode = "round_up",
                isTemplate = false, sourceTemplateId = null, createdBy = testUserId
            )
        )
        return (result as Result.Success).value
    }

    @Nested
    inner class CreateMealPlan {
        @Test
        fun `create template meal plan returns all fields`() {
            val result = client.create(
                CreateMealPlanParam(
                    planId = null, name = "Weekend Template", servings = 4, scalingMode = "fractional",
                    isTemplate = true, sourceTemplateId = null, createdBy = testUserId
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val mp = (result as Result.Success).value
            assertThat(mp.id).isNotNull()
            assertThat(mp.planId).isNull()
            assertThat(mp.name).isEqualTo("Weekend Template")
            assertThat(mp.servings).isEqualTo(4)
            assertThat(mp.scalingMode).isEqualTo("fractional")
            assertThat(mp.isTemplate).isTrue()
            assertThat(mp.sourceTemplateId).isNull()
            assertThat(mp.createdBy).isEqualTo(testUserId)
            assertThat(mp.createdAt).isNotNull()
            assertThat(mp.updatedAt).isNotNull()
        }

        @Test
        fun `create trip-bound meal plan with planId`() {
            val result = client.create(
                CreateMealPlanParam(
                    planId = testPlanId, name = "Trip Meals", servings = 6, scalingMode = "round_up",
                    isTemplate = false, sourceTemplateId = null, createdBy = testUserId
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val mp = (result as Result.Success).value
            assertThat(mp.planId).isEqualTo(testPlanId)
            assertThat(mp.isTemplate).isFalse()
            assertThat(mp.scalingMode).isEqualTo("round_up")
        }

        @Test
        fun `create meal plan with sourceTemplateId`() {
            val template = createTemplate()
            val result = client.create(
                CreateMealPlanParam(
                    planId = testPlanId, name = "From Template", servings = 4, scalingMode = "fractional",
                    isTemplate = false, sourceTemplateId = template.id, createdBy = testUserId
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val mp = (result as Result.Success).value
            assertThat(mp.sourceTemplateId).isEqualTo(template.id)
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `getById returns meal plan when it exists`() {
            val created = createTemplate()
            val result = client.getById(GetByIdParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found.id).isEqualTo(created.id)
            assertThat(found.name).isEqualTo(created.name)
            assertThat(found.servings).isEqualTo(created.servings)
        }

        @Test
        fun `getById returns NotFoundError when meal plan does not exist`() {
            val result = client.getById(GetByIdParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class GetByPlanId {
        @Test
        fun `getByPlanId returns meal plan when it exists`() {
            val created = createTripMealPlan()
            val result = client.getByPlanId(GetByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found).isNotNull
            assertThat(found!!.id).isEqualTo(created.id)
            assertThat(found.planId).isEqualTo(testPlanId)
        }

        @Test
        fun `getByPlanId returns null when no meal plan for plan`() {
            val result = client.getByPlanId(GetByPlanIdParam(testPlanId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val found = (result as Result.Success).value
            assertThat(found).isNull()
        }
    }

    @Nested
    inner class GetTemplates {
        @Test
        fun `getTemplates returns only templates`() {
            createTemplate("Template A")
            createTemplate("Template B")
            createTripMealPlan()

            val result = client.getTemplates()
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val templates = (result as Result.Success).value
            assertThat(templates).hasSize(2)
            assertThat(templates.map { it.name }).containsExactlyInAnyOrder("Template A", "Template B")
            assertThat(templates.all { it.isTemplate }).isTrue()
        }

        @Test
        fun `getTemplates returns empty list when no templates exist`() {
            createTripMealPlan()

            val result = client.getTemplates()
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val templates = (result as Result.Success).value
            assertThat(templates).isEmpty()
        }
    }

    @Nested
    inner class UpdateMealPlan {
        @Test
        fun `update name only`() {
            val created = createTemplate("Original")
            val result = client.update(UpdateMealPlanParam(id = created.id, name = "Updated"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.name).isEqualTo("Updated")
            assertThat(updated.servings).isEqualTo(created.servings)
            assertThat(updated.scalingMode).isEqualTo(created.scalingMode)
        }

        @Test
        fun `update servings only`() {
            val created = createTemplate(servings = 4)
            val result = client.update(UpdateMealPlanParam(id = created.id, servings = 8))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.servings).isEqualTo(8)
            assertThat(updated.name).isEqualTo(created.name)
        }

        @Test
        fun `update scalingMode only`() {
            val created = createTemplate()
            val result = client.update(UpdateMealPlanParam(id = created.id, scalingMode = "round_up"))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.scalingMode).isEqualTo("round_up")
        }

        @Test
        fun `update all fields together`() {
            val created = createTemplate()
            val result = client.update(
                UpdateMealPlanParam(id = created.id, name = "New Name", servings = 12, scalingMode = "round_up")
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.name).isEqualTo("New Name")
            assertThat(updated.servings).isEqualTo(12)
            assertThat(updated.scalingMode).isEqualTo("round_up")
        }

        @Test
        fun `update returns NotFoundError when meal plan does not exist`() {
            val result = client.update(UpdateMealPlanParam(id = UUID.randomUUID(), name = "Nope"))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class DeleteMealPlan {
        @Test
        fun `delete returns success when meal plan exists`() {
            val created = createTemplate()
            val result = client.delete(DeleteMealPlanParam(created.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val getResult = client.getById(GetByIdParam(created.id))
            assertThat(getResult).isInstanceOf(Result.Failure::class.java)
        }

        @Test
        fun `delete returns NotFoundError when meal plan does not exist`() {
            val result = client.delete(DeleteMealPlanParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class UniqueConstraints {
        @Test
        fun `create two meal plans for same planId returns conflict`() {
            createTripMealPlan()
            val result = client.create(
                CreateMealPlanParam(
                    planId = testPlanId, name = "Second", servings = 2, scalingMode = "fractional",
                    isTemplate = false, sourceTemplateId = null, createdBy = testUserId
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `create meal plan with same planId after deleting first succeeds`() {
            val first = createTripMealPlan()
            client.delete(DeleteMealPlanParam(first.id))

            val result = client.create(
                CreateMealPlanParam(
                    planId = testPlanId, name = "Replacement", servings = 4, scalingMode = "fractional",
                    isTemplate = false, sourceTemplateId = null, createdBy = testUserId
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val mp = (result as Result.Success).value
            assertThat(mp.planId).isEqualTo(testPlanId)
            assertThat(mp.name).isEqualTo("Replacement")
        }
    }

    @Nested
    inner class Days {
        @Test
        fun `addDay returns created day`() {
            val mp = createTemplate()
            val result = client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val day = (result as Result.Success).value
            assertThat(day.id).isNotNull()
            assertThat(day.mealPlanId).isEqualTo(mp.id)
            assertThat(day.dayNumber).isEqualTo(1)
            assertThat(day.createdAt).isNotNull()
            assertThat(day.updatedAt).isNotNull()
        }

        @Test
        fun `addDay duplicate day number returns conflict`() {
            val mp = createTemplate()
            client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1))
            val result = client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `getDays returns days ordered by day number`() {
            val mp = createTemplate()
            client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 3))
            client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1))
            client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 2))

            val result = client.getDays(GetDaysParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val days = (result as Result.Success).value
            assertThat(days).hasSize(3)
            assertThat(days.map { it.dayNumber }).containsExactly(1, 2, 3)
        }

        @Test
        fun `getDays returns empty list when no days`() {
            val mp = createTemplate()
            val result = client.getDays(GetDaysParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val days = (result as Result.Success).value
            assertThat(days).isEmpty()
        }

        @Test
        fun `removeDay returns success`() {
            val mp = createTemplate()
            val day = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value

            val result = client.removeDay(RemoveDayParam(day.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val days = (client.getDays(GetDaysParam(mealPlanId = mp.id)) as Result.Success).value
            assertThat(days).isEmpty()
        }

        @Test
        fun `removeDay returns NotFoundError when day does not exist`() {
            val result = client.removeDay(RemoveDayParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `removeDay cascades to recipes on that day`() {
            val mp = createTemplate()
            val day = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value
            client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "breakfast", recipeId = testRecipeId))

            client.removeDay(RemoveDayParam(day.id))

            val recipes = (client.getRecipesByMealPlanId(GetRecipesByMealPlanIdParam(mp.id)) as Result.Success).value
            assertThat(recipes).isEmpty()
        }
    }

    @Nested
    inner class Recipes {
        @Test
        fun `addRecipe returns created recipe`() {
            val mp = createTemplate()
            val day = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value

            val result = client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "breakfast", recipeId = testRecipeId))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val recipe = (result as Result.Success).value
            assertThat(recipe.id).isNotNull()
            assertThat(recipe.mealPlanDayId).isEqualTo(day.id)
            assertThat(recipe.mealType).isEqualTo("breakfast")
            assertThat(recipe.recipeId).isEqualTo(testRecipeId)
            assertThat(recipe.createdAt).isNotNull()
        }

        @Test
        fun `getRecipesByDayId returns recipes for a day`() {
            val mp = createTemplate()
            val day = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value
            client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "breakfast", recipeId = testRecipeId))
            client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "dinner", recipeId = testRecipeId))

            val result = client.getRecipesByDayId(GetRecipesByDayIdParam(mealPlanDayId = day.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val recipes = (result as Result.Success).value
            assertThat(recipes).hasSize(2)
            assertThat(recipes.map { it.mealType }).containsExactlyInAnyOrder("breakfast", "dinner")
        }

        @Test
        fun `getRecipesByMealPlanId returns recipes across all days`() {
            val mp = createTemplate()
            val day1 = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value
            val day2 = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 2)) as Result.Success).value
            client.addRecipe(AddRecipeParam(mealPlanDayId = day1.id, mealType = "breakfast", recipeId = testRecipeId))
            client.addRecipe(AddRecipeParam(mealPlanDayId = day2.id, mealType = "lunch", recipeId = testRecipeId))

            val result = client.getRecipesByMealPlanId(GetRecipesByMealPlanIdParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val recipes = (result as Result.Success).value
            assertThat(recipes).hasSize(2)
        }

        @Test
        fun `getRecipesByMealPlanId returns empty list when no recipes`() {
            val mp = createTemplate()
            val result = client.getRecipesByMealPlanId(GetRecipesByMealPlanIdParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val recipes = (result as Result.Success).value
            assertThat(recipes).isEmpty()
        }

        @Test
        fun `removeRecipe returns success`() {
            val mp = createTemplate()
            val day = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value
            val recipe = (client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "breakfast", recipeId = testRecipeId)) as Result.Success).value

            val result = client.removeRecipe(RemoveRecipeParam(recipe.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val recipes = (client.getRecipesByDayId(GetRecipesByDayIdParam(mealPlanDayId = day.id)) as Result.Success).value
            assertThat(recipes).isEmpty()
        }

        @Test
        fun `removeRecipe returns NotFoundError when recipe does not exist`() {
            val result = client.removeRecipe(RemoveRecipeParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `same recipe on different meals is allowed`() {
            val mp = createTemplate()
            val day = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value

            val r1 = client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "breakfast", recipeId = testRecipeId))
            val r2 = client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "dinner", recipeId = testRecipeId))

            assertThat(r1).isInstanceOf(Result.Success::class.java)
            assertThat(r2).isInstanceOf(Result.Success::class.java)
            assertThat((r1 as Result.Success).value.id).isNotEqualTo((r2 as Result.Success).value.id)
        }
    }

    @Nested
    inner class ShoppingListPurchases {
        @Test
        fun `upsertPurchase creates new purchase`() {
            val mp = createTemplate()
            val result = client.upsertPurchase(
                UpsertPurchaseParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    unit = "g", quantityPurchased = BigDecimal("500.00")
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val purchase = (result as Result.Success).value
            assertThat(purchase.id).isNotNull()
            assertThat(purchase.mealPlanId).isEqualTo(mp.id)
            assertThat(purchase.ingredientId).isEqualTo(testIngredientId)
            assertThat(purchase.unit).isEqualTo("g")
            assertThat(purchase.quantityPurchased).isEqualByComparingTo(BigDecimal("500.00"))
            assertThat(purchase.createdAt).isNotNull()
        }

        @Test
        fun `upsertPurchase updates existing purchase with same mealPlanId ingredientId and unit`() {
            val mp = createTemplate()
            client.upsertPurchase(
                UpsertPurchaseParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    unit = "g", quantityPurchased = BigDecimal("500.00")
                )
            )
            val result = client.upsertPurchase(
                UpsertPurchaseParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    unit = "g", quantityPurchased = BigDecimal("1000.00")
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val purchase = (result as Result.Success).value
            assertThat(purchase.quantityPurchased).isEqualByComparingTo(BigDecimal("1000.00"))

            val all = (client.getPurchases(GetPurchasesParam(mealPlanId = mp.id)) as Result.Success).value
            assertThat(all).hasSize(1)
        }

        @Test
        fun `getPurchases returns all purchases for meal plan`() {
            val mp = createTemplate()
            val ingredient2Id = UUID.randomUUID()
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate("INSERT INTO ingredients (id, name, category, default_unit) VALUES (:id, :name, :category, :defaultUnit)")
                    .bind("id", ingredient2Id).bind("name", "Pepper").bind("category", "spice").bind("defaultUnit", "g").execute()
            }

            client.upsertPurchase(UpsertPurchaseParam(mealPlanId = mp.id, ingredientId = testIngredientId, unit = "g", quantityPurchased = BigDecimal("100")))
            client.upsertPurchase(UpsertPurchaseParam(mealPlanId = mp.id, ingredientId = ingredient2Id, unit = "g", quantityPurchased = BigDecimal("50")))

            val result = client.getPurchases(GetPurchasesParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val purchases = (result as Result.Success).value
            assertThat(purchases).hasSize(2)
        }

        @Test
        fun `getPurchases returns empty list when no purchases`() {
            val mp = createTemplate()
            val result = client.getPurchases(GetPurchasesParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val purchases = (result as Result.Success).value
            assertThat(purchases).isEmpty()
        }

        @Test
        fun `deletePurchases clears all purchases for meal plan`() {
            val mp = createTemplate()
            client.upsertPurchase(UpsertPurchaseParam(mealPlanId = mp.id, ingredientId = testIngredientId, unit = "g", quantityPurchased = BigDecimal("100")))

            val result = client.deletePurchases(DeletePurchasesParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val purchases = (client.getPurchases(GetPurchasesParam(mealPlanId = mp.id)) as Result.Success).value
            assertThat(purchases).isEmpty()
        }
    }

    @Nested
    inner class CascadeDeletes {
        @Test
        fun `delete meal plan cascades to days`() {
            val mp = createTemplate()
            client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1))
            client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 2))

            client.delete(DeleteMealPlanParam(mp.id))

            val count = jdbi.withHandle<Long, Exception> { handle ->
                handle.createQuery("SELECT count(*) FROM meal_plan_days WHERE meal_plan_id = :id")
                    .bind("id", mp.id)
                    .mapTo(Long::class.java)
                    .one()
            }
            assertThat(count).isEqualTo(0)
        }

        @Test
        fun `delete meal plan cascades to recipes via days`() {
            val mp = createTemplate()
            val day = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value
            client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "breakfast", recipeId = testRecipeId))

            client.delete(DeleteMealPlanParam(mp.id))

            val count = jdbi.withHandle<Long, Exception> { handle ->
                handle.createQuery("SELECT count(*) FROM meal_plan_recipes WHERE meal_plan_day_id = :dayId")
                    .bind("dayId", day.id)
                    .mapTo(Long::class.java)
                    .one()
            }
            assertThat(count).isEqualTo(0)
        }

        @Test
        fun `delete meal plan cascades to purchases`() {
            val mp = createTemplate()
            client.upsertPurchase(UpsertPurchaseParam(mealPlanId = mp.id, ingredientId = testIngredientId, unit = "g", quantityPurchased = BigDecimal("100")))

            client.delete(DeleteMealPlanParam(mp.id))

            val count = jdbi.withHandle<Long, Exception> { handle ->
                handle.createQuery("SELECT count(*) FROM shopping_list_purchases WHERE meal_plan_id = :id")
                    .bind("id", mp.id)
                    .mapTo(Long::class.java)
                    .one()
            }
            assertThat(count).isEqualTo(0)
        }

        @Test
        fun `delete day cascades to recipes on that day`() {
            val mp = createTemplate()
            val day = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value
            client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "breakfast", recipeId = testRecipeId))
            client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "lunch", recipeId = testRecipeId))

            client.removeDay(RemoveDayParam(day.id))

            val count = jdbi.withHandle<Long, Exception> { handle ->
                handle.createQuery("SELECT count(*) FROM meal_plan_recipes WHERE meal_plan_day_id = :dayId")
                    .bind("dayId", day.id)
                    .mapTo(Long::class.java)
                    .one()
            }
            assertThat(count).isEqualTo(0)
        }
    }

    @Nested
    inner class ForeignKeyConstraints {
        @Test
        fun `create meal plan with invalid createdBy throws`() {
            val bogusUserId = UUID.randomUUID()
            val result = try {
                client.create(
                    CreateMealPlanParam(
                        planId = null, name = "Bad", servings = 4, scalingMode = "fractional",
                        isTemplate = true, sourceTemplateId = null, createdBy = bogusUserId
                    )
                )
                null
            } catch (e: Exception) {
                e
            }
            assertThat(result).isNotNull()
            assertThat(result!!.message).contains("fk_meal_plans_created_by")
        }

        @Test
        fun `addRecipe with invalid recipeId throws`() {
            val mp = createTemplate()
            val day = (client.addDay(AddDayParam(mealPlanId = mp.id, dayNumber = 1)) as Result.Success).value
            val bogusRecipeId = UUID.randomUUID()

            val result = try {
                client.addRecipe(AddRecipeParam(mealPlanDayId = day.id, mealType = "breakfast", recipeId = bogusRecipeId))
                null
            } catch (e: Exception) {
                e
            }
            assertThat(result).isNotNull()
            assertThat(result!!.message).contains("fk_meal_plan_recipes_recipe")
        }

        @Test
        fun `upsertPurchase with invalid ingredientId throws`() {
            val mp = createTemplate()
            val bogusIngredientId = UUID.randomUUID()

            val result = try {
                client.upsertPurchase(
                    UpsertPurchaseParam(
                        mealPlanId = mp.id, ingredientId = bogusIngredientId,
                        unit = "g", quantityPurchased = BigDecimal("100")
                    )
                )
                null
            } catch (e: Exception) {
                e
            }
            assertThat(result).isNotNull()
            assertThat(result!!.message).contains("fk_shopping_list_purchases_ingredient")
        }
    }

    @Nested
    inner class AddManualItem {
        @Test
        fun `add ingredient-based manual item returns all fields`() {
            val mp = createTemplate()
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("250"), unit = "g"
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val item = (result as Result.Success).value
            assertThat(item.id).isNotNull()
            assertThat(item.mealPlanId).isEqualTo(mp.id)
            assertThat(item.ingredientId).isEqualTo(testIngredientId)
            assertThat(item.description).isNull()
            assertThat(item.quantity).isEqualByComparingTo(BigDecimal("250"))
            assertThat(item.unit).isEqualTo("g")
            assertThat(item.quantityPurchased).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(item.createdAt).isNotNull()
            assertThat(item.updatedAt).isNotNull()
        }

        @Test
        fun `add free-form manual item returns all fields`() {
            val mp = createTemplate()
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Paper plates", quantity = BigDecimal("1"), unit = null
                )
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val item = (result as Result.Success).value
            assertThat(item.ingredientId).isNull()
            assertThat(item.description).isEqualTo("Paper plates")
            assertThat(item.quantity).isEqualByComparingTo(BigDecimal("1"))
            assertThat(item.unit).isNull()
        }

        @Test
        fun `duplicate ingredient and unit combo returns ConflictError`() {
            val mp = createTemplate()
            client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("100"), unit = "g"
                )
            )
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("200"), unit = "g"
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ConflictError::class.java)
        }

        @Test
        fun `same ingredient with different units is allowed`() {
            val mp = createTemplate()
            val r1 = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("100"), unit = "g"
                )
            )
            val r2 = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("2"), unit = "kg"
                )
            )
            assertThat(r1).isInstanceOf(Result.Success::class.java)
            assertThat(r2).isInstanceOf(Result.Success::class.java)
        }

        @Test
        fun `zero quantity returns ValidationError`() {
            val mp = createTemplate()
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal.ZERO, unit = "g"
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `negative quantity returns ValidationError`() {
            val mp = createTemplate()
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("-1"), unit = "g"
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `invalid unit returns ValidationError`() {
            val mp = createTemplate()
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("100"), unit = "invalid_unit"
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `ingredient-based item without unit returns ValidationError`() {
            val mp = createTemplate()
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("100"), unit = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `blank description returns ValidationError`() {
            val mp = createTemplate()
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "   ", quantity = BigDecimal("1"), unit = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `description exceeding 500 chars returns ValidationError`() {
            val mp = createTemplate()
            val longDescription = "a".repeat(501)
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = longDescription, quantity = BigDecimal("1"), unit = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `neither ingredientId nor description returns ValidationError`() {
            val mp = createTemplate()
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = null, quantity = BigDecimal("1"), unit = null
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `both ingredientId and description returns ValidationError`() {
            val mp = createTemplate()
            val result = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = "Some text", quantity = BigDecimal("1"), unit = "g"
                )
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `multiple free-form items with same description is allowed`() {
            val mp = createTemplate()
            val r1 = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Paper plates", quantity = BigDecimal("1"), unit = null
                )
            )
            val r2 = client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Paper plates", quantity = BigDecimal("1"), unit = null
                )
            )
            assertThat(r1).isInstanceOf(Result.Success::class.java)
            assertThat(r2).isInstanceOf(Result.Success::class.java)
            assertThat((r1 as Result.Success).value.id).isNotEqualTo((r2 as Result.Success).value.id)
        }
    }

    @Nested
    inner class GetManualItems {
        @Test
        fun `returns empty list when no manual items exist`() {
            val mp = createTemplate()
            val result = client.getManualItems(GetManualItemsParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val items = (result as Result.Success).value
            assertThat(items).isEmpty()
        }

        @Test
        fun `returns manual items for the specified meal plan only`() {
            val mp1 = createTemplate("Plan A")
            val mp2 = createTemplate("Plan B")

            client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp1.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("100"), unit = "g"
                )
            )
            client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp1.id, ingredientId = null,
                    description = "Napkins", quantity = BigDecimal("1"), unit = null
                )
            )
            client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp2.id, ingredientId = null,
                    description = "Cups", quantity = BigDecimal("1"), unit = null
                )
            )

            val result = client.getManualItems(GetManualItemsParam(mealPlanId = mp1.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val items = (result as Result.Success).value
            assertThat(items).hasSize(2)
            assertThat(items.all { it.mealPlanId == mp1.id }).isTrue()
        }

        @Test
        fun `results ordered by created_at`() {
            val mp = createTemplate()
            client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "First item", quantity = BigDecimal("1"), unit = null
                )
            )
            client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Second item", quantity = BigDecimal("1"), unit = null
                )
            )
            client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Third item", quantity = BigDecimal("1"), unit = null
                )
            )

            val result = client.getManualItems(GetManualItemsParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val items = (result as Result.Success).value
            assertThat(items).hasSize(3)
            assertThat(items.map { it.description }).containsExactly("First item", "Second item", "Third item")
            assertThat(items[0].createdAt).isBeforeOrEqualTo(items[1].createdAt)
            assertThat(items[1].createdAt).isBeforeOrEqualTo(items[2].createdAt)
        }
    }

    @Nested
    inner class RemoveManualItem {
        @Test
        fun `successfully removes an existing manual item`() {
            val mp = createTemplate()
            val item = (client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Remove me", quantity = BigDecimal("1"), unit = null
                )
            ) as Result.Success).value

            val result = client.removeManualItem(RemoveManualItemParam(item.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val items = (client.getManualItems(GetManualItemsParam(mealPlanId = mp.id)) as Result.Success).value
            assertThat(items).isEmpty()
        }

        @Test
        fun `returns NotFoundError for non-existent item ID`() {
            val result = client.removeManualItem(RemoveManualItemParam(UUID.randomUUID()))
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }
    }

    @Nested
    inner class UpdateManualItemPurchase {
        @Test
        fun `successfully updates quantity_purchased`() {
            val mp = createTemplate()
            val item = (client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("500"), unit = "g"
                )
            ) as Result.Success).value

            val result = client.updateManualItemPurchase(
                UpdateManualItemPurchaseParam(id = item.id, quantityPurchased = BigDecimal("250"))
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.id).isEqualTo(item.id)
            assertThat(updated.quantityPurchased).isEqualByComparingTo(BigDecimal("250"))
            assertThat(updated.updatedAt).isAfterOrEqualTo(item.updatedAt)
        }

        @Test
        fun `returns NotFoundError for non-existent item ID`() {
            val result = client.updateManualItemPurchase(
                UpdateManualItemPurchaseParam(id = UUID.randomUUID(), quantityPurchased = BigDecimal("10"))
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(NotFoundError::class.java)
        }

        @Test
        fun `negative quantityPurchased returns ValidationError`() {
            val mp = createTemplate()
            val item = (client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Test item", quantity = BigDecimal("1"), unit = null
                )
            ) as Result.Success).value

            val result = client.updateManualItemPurchase(
                UpdateManualItemPurchaseParam(id = item.id, quantityPurchased = BigDecimal("-1"))
            )
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            val error = (result as Result.Failure).error
            assertThat(error).isInstanceOf(ValidationError::class.java)
        }

        @Test
        fun `zero quantityPurchased is allowed`() {
            val mp = createTemplate()
            val item = (client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Test item", quantity = BigDecimal("1"), unit = null
                )
            ) as Result.Success).value

            val result = client.updateManualItemPurchase(
                UpdateManualItemPurchaseParam(id = item.id, quantityPurchased = BigDecimal.ZERO)
            )
            assertThat(result).isInstanceOf(Result.Success::class.java)
            val updated = (result as Result.Success).value
            assertThat(updated.quantityPurchased).isEqualByComparingTo(BigDecimal.ZERO)
        }
    }

    @Nested
    inner class ResetManualItemPurchases {
        @Test
        fun `resets quantity_purchased to 0 for all manual items in the meal plan`() {
            val mp = createTemplate()
            val item1 = (client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = testIngredientId,
                    description = null, quantity = BigDecimal("500"), unit = "g"
                )
            ) as Result.Success).value
            val item2 = (client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Napkins", quantity = BigDecimal("1"), unit = null
                )
            ) as Result.Success).value

            client.updateManualItemPurchase(UpdateManualItemPurchaseParam(id = item1.id, quantityPurchased = BigDecimal("250")))
            client.updateManualItemPurchase(UpdateManualItemPurchaseParam(id = item2.id, quantityPurchased = BigDecimal("1")))

            val result = client.resetManualItemPurchases(ResetManualItemPurchasesParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val items = (client.getManualItems(GetManualItemsParam(mealPlanId = mp.id)) as Result.Success).value
            assertThat(items).hasSize(2)
            assertThat(items.all { it.quantityPurchased.compareTo(BigDecimal.ZERO) == 0 }).isTrue()
        }

        @Test
        fun `does not affect manual items in other meal plans`() {
            val mp1 = createTemplate("Plan A")
            val mp2 = createTemplate("Plan B")

            val item1 = (client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp1.id, ingredientId = null,
                    description = "Plan A item", quantity = BigDecimal("1"), unit = null
                )
            ) as Result.Success).value
            val item2 = (client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp2.id, ingredientId = null,
                    description = "Plan B item", quantity = BigDecimal("1"), unit = null
                )
            ) as Result.Success).value

            client.updateManualItemPurchase(UpdateManualItemPurchaseParam(id = item1.id, quantityPurchased = BigDecimal("1")))
            client.updateManualItemPurchase(UpdateManualItemPurchaseParam(id = item2.id, quantityPurchased = BigDecimal("1")))

            client.resetManualItemPurchases(ResetManualItemPurchasesParam(mealPlanId = mp1.id))

            val mp1Items = (client.getManualItems(GetManualItemsParam(mealPlanId = mp1.id)) as Result.Success).value
            assertThat(mp1Items).hasSize(1)
            assertThat(mp1Items[0].quantityPurchased).isEqualByComparingTo(BigDecimal.ZERO)

            val mp2Items = (client.getManualItems(GetManualItemsParam(mealPlanId = mp2.id)) as Result.Success).value
            assertThat(mp2Items).hasSize(1)
            assertThat(mp2Items[0].quantityPurchased).isEqualByComparingTo(BigDecimal("1"))
        }

        @Test
        fun `reset on meal plan with no manual items succeeds`() {
            val mp = createTemplate()
            val result = client.resetManualItemPurchases(ResetManualItemPurchasesParam(mealPlanId = mp.id))
            assertThat(result).isInstanceOf(Result.Success::class.java)
        }
    }

    @Nested
    inner class ManualItemCascadeDeletes {
        @Test
        fun `delete meal plan cascades to manual items`() {
            val mp = createTemplate()
            client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = null,
                    description = "Cascade me", quantity = BigDecimal("1"), unit = null
                )
            )

            client.delete(DeleteMealPlanParam(mp.id))

            val count = jdbi.withHandle<Long, Exception> { handle ->
                handle.createQuery("SELECT count(*) FROM shopping_list_manual_items WHERE meal_plan_id = :id")
                    .bind("id", mp.id)
                    .mapTo(Long::class.java)
                    .one()
            }
            assertThat(count).isEqualTo(0)
        }

        @Test
        fun `delete ingredient cascades to manual items referencing it`() {
            val mp = createTemplate()
            val ingredientId = UUID.randomUUID()
            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate("INSERT INTO ingredients (id, name, category, default_unit) VALUES (:id, :name, :category, :defaultUnit)")
                    .bind("id", ingredientId).bind("name", "Butter").bind("category", "dairy").bind("defaultUnit", "g").execute()
            }
            client.addManualItem(
                AddManualItemParam(
                    mealPlanId = mp.id, ingredientId = ingredientId,
                    description = null, quantity = BigDecimal("200"), unit = "g"
                )
            )

            jdbi.withHandle<Unit, Exception> { handle ->
                handle.createUpdate("DELETE FROM ingredients WHERE id = :id").bind("id", ingredientId).execute()
            }

            val items = (client.getManualItems(GetManualItemsParam(mealPlanId = mp.id)) as Result.Success).value
            assertThat(items).isEmpty()
        }
    }
}
