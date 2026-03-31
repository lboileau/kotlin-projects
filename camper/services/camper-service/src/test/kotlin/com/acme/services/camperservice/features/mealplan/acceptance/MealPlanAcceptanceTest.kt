package com.acme.services.camperservice.features.mealplan.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.mealplan.acceptance.fixture.MealPlanFixture
import com.acme.services.camperservice.features.mealplan.dto.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerConfig::class)
class MealPlanAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: MealPlanFixture
    private lateinit var userId: UUID
    private lateinit var planId: UUID

    private val mapper = jacksonObjectMapper().apply {
        findAndRegisterModules()
    }

    @BeforeEach
    fun setUp() {
        fixture = MealPlanFixture(jdbcTemplate)
        fixture.truncateAll()
        userId = fixture.insertUser(email = "user@example.com", username = "testuser")
        planId = fixture.insertPlan(name = "Camping Trip", ownerId = userId)
        fixture.insertPlanMember(planId = planId, userId = userId)
    }

    // --- Helpers ---

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        headers.set("Content-Type", "application/json")
        return HttpEntity(body, headers)
    }

    private fun createMealPlanViaApi(
        name: String = "Test Meal Plan",
        servings: Int = 4,
        scalingMode: String? = null,
        isTemplate: Boolean? = null,
        planId: UUID? = null
    ): MealPlanResponse {
        val response = restTemplate.exchange(
            "/api/meal-plans",
            HttpMethod.POST,
            entityWithUser(CreateMealPlanRequest(name, servings, scalingMode, isTemplate, planId), userId),
            MealPlanResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        return response.body!!
    }

    private fun addDayViaApi(mealPlanId: UUID, dayNumber: Int): MealPlanDayResponse {
        val response = restTemplate.exchange(
            "/api/meal-plans/$mealPlanId/days",
            HttpMethod.POST,
            entityWithUser(AddDayRequest(dayNumber), userId),
            MealPlanDayResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        return response.body!!
    }

    private fun addRecipeViaApi(mealPlanId: UUID, dayId: UUID, mealType: String, recipeId: UUID): MealPlanRecipeDetailResponse {
        val response = restTemplate.exchange(
            "/api/meal-plans/$mealPlanId/days/$dayId/recipes",
            HttpMethod.POST,
            entityWithUser(AddRecipeRequest(mealType, recipeId), userId),
            MealPlanRecipeDetailResponse::class.java
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        return response.body!!
    }

    // --- CRUD Tests ---

    @Nested
    inner class CreateMealPlan {

        @Test
        fun `POST creates trip meal plan and returns 201`() {
            val response = restTemplate.exchange(
                "/api/meal-plans",
                HttpMethod.POST,
                entityWithUser(
                    CreateMealPlanRequest(name = "Trip Meals", servings = 6, scalingMode = "round_up", isTemplate = false, planId = planId),
                    userId
                ),
                MealPlanResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.name).isEqualTo("Trip Meals")
            assertThat(body.servings).isEqualTo(6)
            assertThat(body.scalingMode).isEqualTo("round_up")
            assertThat(body.isTemplate).isFalse()
            assertThat(body.planId).isEqualTo(planId)
            assertThat(body.createdBy).isEqualTo(userId)
        }

        @Test
        fun `POST creates template meal plan and returns 201`() {
            val response = restTemplate.exchange(
                "/api/meal-plans",
                HttpMethod.POST,
                entityWithUser(
                    CreateMealPlanRequest(name = "My Template", servings = 4, scalingMode = null, isTemplate = true, planId = null),
                    userId
                ),
                MealPlanResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.name).isEqualTo("My Template")
            assertThat(body.isTemplate).isTrue()
            assertThat(body.planId).isNull()
            assertThat(body.scalingMode).isEqualTo("fractional")
        }

        @Test
        fun `POST returns 400 when name is blank`() {
            val response = restTemplate.exchange(
                "/api/meal-plans",
                HttpMethod.POST,
                entityWithUser(
                    CreateMealPlanRequest(name = "", servings = 4, scalingMode = null, isTemplate = null, planId = null),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 400 when servings is zero`() {
            val response = restTemplate.exchange(
                "/api/meal-plans",
                HttpMethod.POST,
                entityWithUser(
                    CreateMealPlanRequest(name = "Bad Plan", servings = 0, scalingMode = null, isTemplate = null, planId = null),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 400 when scaling mode is invalid`() {
            val response = restTemplate.exchange(
                "/api/meal-plans",
                HttpMethod.POST,
                entityWithUser(
                    CreateMealPlanRequest(name = "Bad Plan", servings = 4, scalingMode = "invalid_mode", isTemplate = null, planId = null),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 409 when plan already has a meal plan`() {
            createMealPlanViaApi(name = "First", planId = planId)

            val response = restTemplate.exchange(
                "/api/meal-plans",
                HttpMethod.POST,
                entityWithUser(
                    CreateMealPlanRequest(name = "Second", servings = 4, scalingMode = null, isTemplate = null, planId = planId),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @Nested
    inner class GetMealPlan {

        @Test
        fun `GET by ID returns 200 with meal plan detail`() {
            val mealPlan = createMealPlanViaApi(name = "My Plan", servings = 4, planId = planId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                MealPlanDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.id).isEqualTo(mealPlan.id)
            assertThat(body.name).isEqualTo("My Plan")
            assertThat(body.days).isEmpty()
        }

        @Test
        fun `GET by ID returns 404 for non-existent meal plan`() {
            val response = restTemplate.exchange(
                "/api/meal-plans/${UUID.randomUUID()}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `GET by planId returns 200 with meal plan detail`() {
            val mealPlan = createMealPlanViaApi(name = "Trip Plan", planId = planId)

            val response = restTemplate.exchange(
                "/api/meal-plans?planId=$planId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                MealPlanDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.id).isEqualTo(mealPlan.id)
        }

        @Test
        fun `GET by planId returns 200 with null when no meal plan exists`() {
            val response = restTemplate.exchange(
                "/api/meal-plans?planId=$planId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                String::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        fun `GET templates returns 200 with template list`() {
            createMealPlanViaApi(name = "Template A", isTemplate = true)
            createMealPlanViaApi(name = "Template B", isTemplate = true)
            createMealPlanViaApi(name = "Trip Plan", planId = planId)

            val response = restTemplate.exchange(
                "/api/meal-plans/templates",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<MealPlanResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val names = response.body!!.map { it.name }
            assertThat(names).containsExactlyInAnyOrder("Template A", "Template B")
        }
    }

    @Nested
    inner class UpdateMealPlan {

        @Test
        fun `PUT returns 200 with updated meal plan`() {
            val mealPlan = createMealPlanViaApi(name = "Old Name", servings = 4, planId = planId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.PUT,
                entityWithUser(UpdateMealPlanRequest(name = "New Name", servings = 8, scalingMode = "round_up"), userId),
                MealPlanResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.name).isEqualTo("New Name")
            assertThat(body.servings).isEqualTo(8)
            assertThat(body.scalingMode).isEqualTo("round_up")
        }

        @Test
        fun `PUT returns 404 when meal plan not found`() {
            val response = restTemplate.exchange(
                "/api/meal-plans/${UUID.randomUUID()}",
                HttpMethod.PUT,
                entityWithUser(UpdateMealPlanRequest(name = "Nope", servings = null, scalingMode = null), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class DeleteMealPlan {

        @Test
        fun `DELETE returns 204`() {
            val mealPlan = createMealPlanViaApi(name = "To Delete", planId = planId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // Verify it's gone
            val getResponse = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Map::class.java
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `DELETE returns 404 when meal plan not found`() {
            val response = restTemplate.exchange(
                "/api/meal-plans/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    // --- Day Tests ---

    @Nested
    inner class DayOperations {

        @Test
        fun `POST adds day and returns 201`() {
            val mealPlan = createMealPlanViaApi(name = "With Days", planId = planId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/days",
                HttpMethod.POST,
                entityWithUser(AddDayRequest(dayNumber = 1), userId),
                MealPlanDayResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.dayNumber).isEqualTo(1)
        }

        @Test
        fun `POST returns 409 for duplicate day number`() {
            val mealPlan = createMealPlanViaApi(name = "Dup Day", planId = planId)
            addDayViaApi(mealPlan.id, 1)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/days",
                HttpMethod.POST,
                entityWithUser(AddDayRequest(dayNumber = 1), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        fun `DELETE removes day and cascades recipes, returns 204`() {
            val mealPlan = createMealPlanViaApi(name = "Cascade", planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            val recipeId = fixture.insertRecipe(name = "Pancakes", baseServings = 4, createdBy = userId)
            addRecipeViaApi(mealPlan.id, day.id, "breakfast", recipeId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/days/${day.id}",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // Verify day is gone from detail
            val detail = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                MealPlanDetailResponse::class.java
            )
            assertThat(detail.body!!.days).isEmpty()
        }
    }

    // --- Recipe Tests ---

    @Nested
    inner class RecipeOperations {

        @Test
        fun `POST adds recipe to meal and returns 201`() {
            val mealPlan = createMealPlanViaApi(name = "With Recipes", planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            val recipeId = fixture.insertRecipe(name = "Omelette", baseServings = 2, createdBy = userId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/days/${day.id}/recipes",
                HttpMethod.POST,
                entityWithUser(AddRecipeRequest(mealType = "breakfast", recipeId = recipeId), userId),
                MealPlanRecipeDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.recipeId).isEqualTo(recipeId)
            assertThat(response.body!!.recipeName).isEqualTo("Omelette")
        }

        @Test
        fun `DELETE removes recipe from meal and returns 204`() {
            val mealPlan = createMealPlanViaApi(name = "Remove Recipe", planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            val recipeId = fixture.insertRecipe(name = "Salad", baseServings = 2, createdBy = userId)
            val mealPlanRecipe = addRecipeViaApi(mealPlan.id, day.id, "lunch", recipeId)

            val response = restTemplate.exchange(
                "/api/meal-plan-recipes/${mealPlanRecipe.id}",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // Verify recipe is gone
            val detail = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                MealPlanDetailResponse::class.java
            )
            assertThat(detail.body!!.days[0].meals.lunch).isEmpty()
        }
    }

    // --- Shopping List Tests ---

    @Nested
    inner class ShoppingListOperations {

        @Test
        fun `GET shopping list returns 200 with empty categories when no recipes`() {
            val mealPlan = createMealPlanViaApi(name = "Empty Plan", planId = planId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.categories).isEmpty()
            assertThat(response.body!!.totalItems).isEqualTo(0)
        }

        @Test
        fun `GET shopping list returns computed quantities`() {
            val ingredientId = fixture.insertIngredient(name = "Flour", category = "pantry", defaultUnit = "cup")
            val recipeId = fixture.insertRecipe(name = "Pancakes", baseServings = 4, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId, quantity = BigDecimal("2"), unit = "cup")

            val mealPlan = createMealPlanViaApi(name = "Shopping Test", servings = 4, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            addRecipeViaApi(mealPlan.id, day.id, "breakfast", recipeId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.totalItems).isEqualTo(1)
            val items = body.categories.flatMap { it.items }
            assertThat(items).hasSize(1)
            assertThat(items[0].ingredientName).isEqualTo("Flour")
            // 4 servings / 4 base servings = 1x scale, so 2 cups
            assertThat(items[0].quantityRequired).isEqualByComparingTo(BigDecimal("2"))
            assertThat(items[0].unit).isEqualTo("cup")
            assertThat(items[0].status).isEqualTo("not_purchased")
        }

        @Test
        fun `PATCH updates purchase and returns 200`() {
            val ingredientId = fixture.insertIngredient(name = "Sugar", category = "pantry", defaultUnit = "cup")
            val recipeId = fixture.insertRecipe(name = "Cake", baseServings = 4, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId, quantity = BigDecimal("1"), unit = "cup")

            val mealPlan = createMealPlanViaApi(name = "Purchase Test", servings = 4, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            addRecipeViaApi(mealPlan.id, day.id, "dinner", recipeId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.PATCH,
                entityWithUser(
                    UpdatePurchaseRequest(ingredientId = ingredientId, unit = "cup", quantityPurchased = BigDecimal("1")),
                    userId
                ),
                ShoppingListPurchaseResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.ingredientId).isEqualTo(ingredientId)
            assertThat(response.body!!.quantityPurchased).isEqualByComparingTo(BigDecimal("1"))

            // Verify status is now "done"
            val listResponse = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val items = listResponse.body!!.categories.flatMap { it.items }
            assertThat(items[0].status).isEqualTo("done")
            assertThat(listResponse.body!!.fullyPurchasedCount).isEqualTo(1)
        }

        @Test
        fun `PATCH upsert updates existing purchase`() {
            val ingredientId = fixture.insertIngredient(name = "Milk", category = "dairy", defaultUnit = "cup")
            val recipeId = fixture.insertRecipe(name = "Cereal", baseServings = 2, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId, quantity = BigDecimal("2"), unit = "cup")

            val mealPlan = createMealPlanViaApi(name = "Upsert Test", servings = 2, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            addRecipeViaApi(mealPlan.id, day.id, "breakfast", recipeId)

            // First purchase: partial
            restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.PATCH,
                entityWithUser(UpdatePurchaseRequest(ingredientId = ingredientId, unit = "cup", quantityPurchased = BigDecimal("1")), userId),
                ShoppingListPurchaseResponse::class.java
            )

            // Second purchase: update to full
            restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.PATCH,
                entityWithUser(UpdatePurchaseRequest(ingredientId = ingredientId, unit = "cup", quantityPurchased = BigDecimal("2")), userId),
                ShoppingListPurchaseResponse::class.java
            )

            val listResponse = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val items = listResponse.body!!.categories.flatMap { it.items }
            assertThat(items[0].quantityPurchased).isEqualByComparingTo(BigDecimal("2"))
            assertThat(items[0].status).isEqualTo("done")
        }

        @Test
        fun `DELETE resets all purchases and returns 204`() {
            val ingredientId = fixture.insertIngredient(name = "Eggs", category = "dairy", defaultUnit = "pieces")
            val recipeId = fixture.insertRecipe(name = "Scramble", baseServings = 2, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId, quantity = BigDecimal("4"), unit = "pieces")

            val mealPlan = createMealPlanViaApi(name = "Reset Test", servings = 2, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            addRecipeViaApi(mealPlan.id, day.id, "breakfast", recipeId)

            // Purchase
            restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.PATCH,
                entityWithUser(UpdatePurchaseRequest(ingredientId = ingredientId, unit = "pieces", quantityPurchased = BigDecimal("4")), userId),
                ShoppingListPurchaseResponse::class.java
            )

            // Reset
            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // Verify all not_purchased
            val listResponse = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val items = listResponse.body!!.categories.flatMap { it.items }
            assertThat(items).allSatisfy { item ->
                assertThat(item.status).isEqualTo("not_purchased")
                assertThat(item.quantityPurchased).isEqualByComparingTo(BigDecimal.ZERO)
            }
        }

        @Test
        fun `shopping list shows more_needed when partially purchased`() {
            val ingredientId = fixture.insertIngredient(name = "Rice", category = "pantry", defaultUnit = "cup")
            val recipeId = fixture.insertRecipe(name = "Fried Rice", baseServings = 4, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId, quantity = BigDecimal("3"), unit = "cup")

            val mealPlan = createMealPlanViaApi(name = "Partial Purchase", servings = 4, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            addRecipeViaApi(mealPlan.id, day.id, "dinner", recipeId)

            // Partially purchase
            restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.PATCH,
                entityWithUser(UpdatePurchaseRequest(ingredientId = ingredientId, unit = "cup", quantityPurchased = BigDecimal("1")), userId),
                ShoppingListPurchaseResponse::class.java
            )

            val listResponse = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val items = listResponse.body!!.categories.flatMap { it.items }
            assertThat(items[0].status).isEqualTo("more_needed")
            assertThat(items[0].quantityPurchased).isEqualByComparingTo(BigDecimal("1"))
            assertThat(items[0].quantityRequired).isEqualByComparingTo(BigDecimal("3"))
        }

        @Test
        fun `removing recipe from meal plan updates shopping list quantities`() {
            val ingredientId = fixture.insertIngredient(name = "Butter", category = "dairy", defaultUnit = "tbsp")
            val recipe1 = fixture.insertRecipe(name = "Toast", baseServings = 4, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipe1, ingredientId = ingredientId, quantity = BigDecimal("2"), unit = "tbsp")
            val recipe2 = fixture.insertRecipe(name = "Pancakes", baseServings = 4, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipe2, ingredientId = ingredientId, quantity = BigDecimal("3"), unit = "tbsp")

            val mealPlan = createMealPlanViaApi(name = "Live Changes", servings = 4, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            addRecipeViaApi(mealPlan.id, day.id, "breakfast", recipe1)
            val mpr2 = addRecipeViaApi(mealPlan.id, day.id, "breakfast", recipe2)

            // Both recipes contribute: 2 + 3 = 5 tbsp
            val before = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val beforeItems = before.body!!.categories.flatMap { it.items }
            assertThat(beforeItems).hasSize(1)
            assertThat(beforeItems[0].quantityRequired).isEqualByComparingTo(BigDecimal("5"))

            // Remove second recipe
            restTemplate.exchange(
                "/api/meal-plan-recipes/${mpr2.id}",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )

            // Shopping list should now reflect only recipe1: 2 tbsp
            val after = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val afterItems = after.body!!.categories.flatMap { it.items }
            assertThat(afterItems).hasSize(1)
            assertThat(afterItems[0].quantityRequired).isEqualByComparingTo(BigDecimal("2"))
        }
    }

    // --- Copy to Trip ---

    @Nested
    inner class CopyToTrip {

        @Test
        fun `POST copies template to trip and returns 201`() {
            // Create template with days and recipes
            val template = createMealPlanViaApi(name = "Weekend Template", servings = 4, isTemplate = true)
            val day = addDayViaApi(template.id, 1)
            val recipeId = fixture.insertRecipe(name = "Grilled Chicken", baseServings = 4, createdBy = userId)
            addRecipeViaApi(template.id, day.id, "dinner", recipeId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${template.id}/copy-to-trip",
                HttpMethod.POST,
                entityWithUser(CopyToTripRequest(planId = planId, servings = 6), userId),
                MealPlanDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.planId).isEqualTo(planId)
            assertThat(body.isTemplate).isFalse()
            assertThat(body.servings).isEqualTo(6)
            assertThat(body.sourceTemplateId).isEqualTo(template.id)
            assertThat(body.days).hasSize(1)
            assertThat(body.days[0].dayNumber).isEqualTo(1)
            assertThat(body.days[0].meals.dinner).hasSize(1)
            assertThat(body.days[0].meals.dinner[0].recipeName).isEqualTo("Grilled Chicken")
        }

        @Test
        fun `POST returns 400 when trying to copy non-template`() {
            val tripMealPlan = createMealPlanViaApi(name = "Trip Plan", planId = planId)

            val otherPlanId = fixture.insertPlan(name = "Other Trip", ownerId = userId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${tripMealPlan.id}/copy-to-trip",
                HttpMethod.POST,
                entityWithUser(CopyToTripRequest(planId = otherPlanId, servings = null), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    // --- Save as Template ---

    @Nested
    inner class SaveAsTemplate {

        @Test
        fun `POST saves trip meal plan as template and returns 201`() {
            val mealPlan = createMealPlanViaApi(name = "Trip Meals", servings = 6, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            val recipeId = fixture.insertRecipe(name = "Chili", baseServings = 4, createdBy = userId)
            addRecipeViaApi(mealPlan.id, day.id, "dinner", recipeId)

            val response = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/save-as-template",
                HttpMethod.POST,
                entityWithUser(SaveAsTemplateRequest(name = "Chili Template"), userId),
                MealPlanResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val body = response.body!!
            assertThat(body.name).isEqualTo("Chili Template")
            assertThat(body.isTemplate).isTrue()
            assertThat(body.planId).isNull()

            // Verify template has same days/recipes
            val detail = restTemplate.exchange(
                "/api/meal-plans/${body.id}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                MealPlanDetailResponse::class.java
            )
            assertThat(detail.body!!.days).hasSize(1)
            assertThat(detail.body!!.days[0].meals.dinner).hasSize(1)
            assertThat(detail.body!!.days[0].meals.dinner[0].recipeName).isEqualTo("Chili")
        }

        @Test
        fun `POST returns 400 when trying to save template as template`() {
            val template = createMealPlanViaApi(name = "Already Template", isTemplate = true)

            val response = restTemplate.exchange(
                "/api/meal-plans/${template.id}/save-as-template",
                HttpMethod.POST,
                entityWithUser(SaveAsTemplateRequest(name = "Duplicate"), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    // --- Headcount Change Flow ---

    @Nested
    inner class HeadcountChangeFlow {

        @Test
        fun `updating servings changes shopping list quantities`() {
            val ingredientId = fixture.insertIngredient(name = "Pasta", category = "pantry", defaultUnit = "g")
            val recipeId = fixture.insertRecipe(name = "Spaghetti", baseServings = 4, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId, quantity = BigDecimal("400"), unit = "g")

            // Create with servings=4
            val mealPlan = createMealPlanViaApi(name = "Headcount Test", servings = 4, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            addRecipeViaApi(mealPlan.id, day.id, "dinner", recipeId)

            // Check quantities at 4 servings (1x scale = 400g)
            val list1 = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val items1 = list1.body!!.categories.flatMap { it.items }
            assertThat(items1[0].quantityRequired).isEqualByComparingTo(BigDecimal("400"))

            // Update servings to 8
            restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.PUT,
                entityWithUser(UpdateMealPlanRequest(name = null, servings = 8, scalingMode = null), userId),
                MealPlanResponse::class.java
            )

            // Check quantities at 8 servings (2x scale = 800g)
            val list2 = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val items2 = list2.body!!.categories.flatMap { it.items }
            assertThat(items2[0].quantityRequired).isEqualByComparingTo(BigDecimal("800"))
        }

        @Test
        fun `headcount change creates purchase shortfall`() {
            val ingredientId = fixture.insertIngredient(name = "Butter", category = "dairy", defaultUnit = "cup")
            val recipeId = fixture.insertRecipe(name = "Garlic Bread", baseServings = 4, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId, quantity = BigDecimal("1"), unit = "cup")

            val mealPlan = createMealPlanViaApi(name = "Shortfall Test", servings = 4, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            addRecipeViaApi(mealPlan.id, day.id, "dinner", recipeId)

            // Purchase exactly enough for 4 servings (1 cup)
            restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.PATCH,
                entityWithUser(UpdatePurchaseRequest(ingredientId = ingredientId, unit = "cup", quantityPurchased = BigDecimal("1")), userId),
                ShoppingListPurchaseResponse::class.java
            )

            // Verify done at servings=4
            val list1 = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            assertThat(list1.body!!.categories.flatMap { it.items }[0].status).isEqualTo("done")

            // Increase to 8 servings
            restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.PUT,
                entityWithUser(UpdateMealPlanRequest(name = null, servings = 8, scalingMode = null), userId),
                MealPlanResponse::class.java
            )

            // Now should be more_needed (required=2 cups, purchased=1 cup)
            val list2 = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val items2 = list2.body!!.categories.flatMap { it.items }
            assertThat(items2[0].status).isEqualTo("more_needed")
            assertThat(items2[0].quantityRequired).isEqualByComparingTo(BigDecimal("2"))
            assertThat(items2[0].quantityPurchased).isEqualByComparingTo(BigDecimal("1"))
        }
    }

    // --- Full Workflow ---

    @Nested
    inner class FullWorkflow {

        @Test
        fun `complete meal plan workflow from template creation to shopping`() {
            // 1. Set up ingredients and recipes
            val flourId = fixture.insertIngredient(name = "Flour", category = "pantry", defaultUnit = "cup")
            val eggsId = fixture.insertIngredient(name = "Eggs", category = "dairy", defaultUnit = "pieces")
            val butterIdent = fixture.insertIngredient(name = "Butter", category = "dairy", defaultUnit = "tbsp")

            val pancakeId = fixture.insertRecipe(name = "Pancakes", baseServings = 4, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = pancakeId, ingredientId = flourId, quantity = BigDecimal("2"), unit = "cup")
            fixture.insertRecipeIngredient(recipeId = pancakeId, ingredientId = eggsId, quantity = BigDecimal("3"), unit = "pieces")

            val grilledCheeseId = fixture.insertRecipe(name = "Grilled Cheese", baseServings = 2, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = grilledCheeseId, ingredientId = butterIdent, quantity = BigDecimal("2"), unit = "tbsp")

            // 2. Create template meal plan
            val template = createMealPlanViaApi(name = "Weekend Camping", servings = 4, isTemplate = true)

            // 3. Add days
            val day1 = addDayViaApi(template.id, 1)
            val day2 = addDayViaApi(template.id, 2)

            // 4. Add recipes to days/meals
            addRecipeViaApi(template.id, day1.id, "breakfast", pancakeId)
            addRecipeViaApi(template.id, day1.id, "lunch", grilledCheeseId)
            addRecipeViaApi(template.id, day2.id, "breakfast", pancakeId) // same recipe again

            // 5. Copy template to trip with different servings
            val copyResponse = restTemplate.exchange(
                "/api/meal-plans/${template.id}/copy-to-trip",
                HttpMethod.POST,
                entityWithUser(CopyToTripRequest(planId = planId, servings = 8), userId),
                MealPlanDetailResponse::class.java
            )
            assertThat(copyResponse.statusCode).isEqualTo(HttpStatus.CREATED)
            val tripMealPlan = copyResponse.body!!
            assertThat(tripMealPlan.servings).isEqualTo(8)
            assertThat(tripMealPlan.days).hasSize(2)

            // 6. Get meal plan detail — verify scaling
            val detailResponse = restTemplate.exchange(
                "/api/meal-plans/${tripMealPlan.id}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                MealPlanDetailResponse::class.java
            )
            assertThat(detailResponse.statusCode).isEqualTo(HttpStatus.OK)
            val detail = detailResponse.body!!
            val breakfastDay1 = detail.days.first { it.dayNumber == 1 }.meals.breakfast
            assertThat(breakfastDay1).hasSize(1)
            assertThat(breakfastDay1[0].recipeName).isEqualTo("Pancakes")
            // Pancakes: 8 servings / 4 base = 2x scale
            assertThat(breakfastDay1[0].scaleFactor).isEqualByComparingTo(BigDecimal("2"))

            // 7. Get shopping list — verify computed quantities
            val shoppingResponse = restTemplate.exchange(
                "/api/meal-plans/${tripMealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            assertThat(shoppingResponse.statusCode).isEqualTo(HttpStatus.OK)
            val shoppingList = shoppingResponse.body!!
            assertThat(shoppingList.servings).isEqualTo(8)
            val allItems = shoppingList.categories.flatMap { it.items }

            // Flour: Pancakes (2 cup * 2x) on day 1 + Pancakes (2 cup * 2x) on day 2 = 8 cups total
            val flour = allItems.first { it.ingredientName == "Flour" }
            assertThat(flour.quantityRequired).isEqualByComparingTo(BigDecimal("8"))
            assertThat(flour.unit).isEqualTo("cup")
            assertThat(flour.usedInRecipes).contains("Pancakes")

            // Eggs: 3 * 2x on day 1 + 3 * 2x on day 2 = 12 pieces total
            val eggs = allItems.first { it.ingredientName == "Eggs" }
            assertThat(eggs.quantityRequired).isEqualByComparingTo(BigDecimal("12"))

            // Butter: Grilled Cheese 2 tbsp * (8/2=4x) = 8 tbsp → bestFit → 0.5 cup
            val butter = allItems.first { it.ingredientName == "Butter" }
            assertThat(butter.quantityRequired).isEqualByComparingTo(BigDecimal("0.5"))
            assertThat(butter.unit).isEqualTo("cup")

            // 8. Update purchases
            restTemplate.exchange(
                "/api/meal-plans/${tripMealPlan.id}/shopping-list",
                HttpMethod.PATCH,
                entityWithUser(UpdatePurchaseRequest(ingredientId = flourId, unit = "cup", quantityPurchased = BigDecimal("8")), userId),
                ShoppingListPurchaseResponse::class.java
            )
            restTemplate.exchange(
                "/api/meal-plans/${tripMealPlan.id}/shopping-list",
                HttpMethod.PATCH,
                entityWithUser(UpdatePurchaseRequest(ingredientId = eggsId, unit = "pieces", quantityPurchased = BigDecimal("6")), userId),
                ShoppingListPurchaseResponse::class.java
            )

            // 9. Get shopping list — verify statuses
            val shoppingResponse2 = restTemplate.exchange(
                "/api/meal-plans/${tripMealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val items2 = shoppingResponse2.body!!.categories.flatMap { it.items }
            val flour2 = items2.first { it.ingredientName == "Flour" }
            assertThat(flour2.status).isEqualTo("done")

            val eggs2 = items2.first { it.ingredientName == "Eggs" }
            assertThat(eggs2.status).isEqualTo("more_needed")
            assertThat(eggs2.quantityPurchased).isEqualByComparingTo(BigDecimal("6"))

            val butter2 = items2.first { it.ingredientName == "Butter" }
            assertThat(butter2.status).isEqualTo("not_purchased")

            // 10. Reset purchases
            val resetResponse = restTemplate.exchange(
                "/api/meal-plans/${tripMealPlan.id}/shopping-list",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )
            assertThat(resetResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // 11. Verify all not_purchased
            val shoppingResponse3 = restTemplate.exchange(
                "/api/meal-plans/${tripMealPlan.id}/shopping-list",
                HttpMethod.GET,
                entityWithUser(null, userId),
                ShoppingListResponse::class.java
            )
            val items3 = shoppingResponse3.body!!.categories.flatMap { it.items }
            assertThat(items3).allSatisfy { item ->
                assertThat(item.status).isEqualTo("not_purchased")
            }
        }
    }

    // --- Detail Response Shape Tests ---

    @Nested
    inner class DetailResponseShape {

        @Test
        fun `meal plan detail shows recipes grouped by meal type`() {
            val recipeA = fixture.insertRecipe(name = "Oatmeal", baseServings = 2, createdBy = userId)
            val recipeB = fixture.insertRecipe(name = "Sandwich", baseServings = 4, createdBy = userId)
            val recipeC = fixture.insertRecipe(name = "Steak", baseServings = 2, createdBy = userId)
            val recipeD = fixture.insertRecipe(name = "Trail Mix", baseServings = 8, createdBy = userId)

            val mealPlan = createMealPlanViaApi(name = "Full Day", servings = 4, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)

            addRecipeViaApi(mealPlan.id, day.id, "breakfast", recipeA)
            addRecipeViaApi(mealPlan.id, day.id, "lunch", recipeB)
            addRecipeViaApi(mealPlan.id, day.id, "dinner", recipeC)
            addRecipeViaApi(mealPlan.id, day.id, "snack", recipeD)

            val detail = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                MealPlanDetailResponse::class.java
            )

            assertThat(detail.statusCode).isEqualTo(HttpStatus.OK)
            val meals = detail.body!!.days[0].meals
            assertThat(meals.breakfast).hasSize(1)
            assertThat(meals.breakfast[0].recipeName).isEqualTo("Oatmeal")
            assertThat(meals.lunch).hasSize(1)
            assertThat(meals.lunch[0].recipeName).isEqualTo("Sandwich")
            assertThat(meals.dinner).hasSize(1)
            assertThat(meals.dinner[0].recipeName).isEqualTo("Steak")
            assertThat(meals.snack).hasSize(1)
            assertThat(meals.snack[0].recipeName).isEqualTo("Trail Mix")
        }

        @Test
        fun `meal plan detail shows ingredient scaling info`() {
            val ingredientId = fixture.insertIngredient(name = "Chicken", category = "meat", defaultUnit = "g")
            val recipeId = fixture.insertRecipe(name = "Grilled Chicken", baseServings = 2, createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId, quantity = BigDecimal("500"), unit = "g")

            val mealPlan = createMealPlanViaApi(name = "Scaling Detail", servings = 6, planId = planId)
            val day = addDayViaApi(mealPlan.id, 1)
            addRecipeViaApi(mealPlan.id, day.id, "dinner", recipeId)

            val detail = restTemplate.exchange(
                "/api/meal-plans/${mealPlan.id}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                MealPlanDetailResponse::class.java
            )

            val recipe = detail.body!!.days[0].meals.dinner[0]
            assertThat(recipe.baseServings).isEqualTo(2)
            assertThat(recipe.scaleFactor).isEqualByComparingTo(BigDecimal("3"))
            assertThat(recipe.ingredients).hasSize(1)
            val ingredient = recipe.ingredients[0]
            assertThat(ingredient.ingredientName).isEqualTo("Chicken")
            assertThat(ingredient.quantity).isEqualByComparingTo(BigDecimal("500"))
            assertThat(ingredient.scaledQuantity).isEqualByComparingTo(BigDecimal("1500"))
        }
    }
}
