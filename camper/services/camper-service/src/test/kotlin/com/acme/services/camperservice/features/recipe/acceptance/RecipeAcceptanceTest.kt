package com.acme.services.camperservice.features.recipe.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.recipe.acceptance.fixture.RecipeFixture
import com.acme.services.camperservice.features.recipe.dto.CreateIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.CreateRecipeIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.CreateRecipeRequest
import com.acme.services.camperservice.features.recipe.dto.ImportRecipeRequest
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.dto.RecipeResponse
import com.acme.services.camperservice.features.recipe.dto.ResolveDuplicateRequest
import com.acme.services.camperservice.features.recipe.dto.ResolveIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.UpdateRecipeRequest
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
class RecipeAcceptanceTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var fixture: RecipeFixture
    private lateinit var userId: UUID
    private lateinit var otherUserId: UUID

    @BeforeEach
    fun setUp() {
        fixture = RecipeFixture(jdbcTemplate)
        fixture.truncateAll()
        userId = fixture.insertUser(email = "creator@example.com", username = "creator")
        otherUserId = fixture.insertUser(email = "other@example.com", username = "other")
    }

    @Nested
    inner class CreateRecipe {

        @Test
        fun `POST returns 201 with created published recipe`() {
            val response = restTemplate.exchange(
                "/api/recipes",
                HttpMethod.POST,
                entityWithUser(
                    CreateRecipeRequest(
                        name = "Classic Pasta",
                        description = "Simple and delicious",
                        webLink = null,
                        baseServings = 4,
                        ingredients = emptyList()
                    ),
                    userId
                ),
                RecipeResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.name).isEqualTo("Classic Pasta")
            assertThat(response.body!!.status).isEqualTo("published")
            assertThat(response.body!!.createdBy).isEqualTo(userId)
            assertThat(response.body!!.baseServings).isEqualTo(4)
        }

        @Test
        fun `POST returns 201 with ingredients`() {
            val ingredientId = fixture.insertIngredient(name = "Garlic", category = "produce", defaultUnit = "clove")

            val response = restTemplate.exchange(
                "/api/recipes",
                HttpMethod.POST,
                entityWithUser(
                    CreateRecipeRequest(
                        name = "Garlic Bread",
                        description = null,
                        webLink = null,
                        baseServings = 2,
                        ingredients = listOf(
                            CreateRecipeIngredientRequest(
                                ingredientId = ingredientId,
                                quantity = BigDecimal("3"),
                                unit = "clove"
                            )
                        )
                    ),
                    userId
                ),
                RecipeResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.name).isEqualTo("Garlic Bread")
        }

        @Test
        fun `POST returns 400 when name is blank`() {
            val response = restTemplate.exchange(
                "/api/recipes",
                HttpMethod.POST,
                entityWithUser(
                    CreateRecipeRequest(name = "", description = null, webLink = null, baseServings = 4, ingredients = emptyList()),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 400 when baseServings is zero`() {
            val response = restTemplate.exchange(
                "/api/recipes",
                HttpMethod.POST,
                entityWithUser(
                    CreateRecipeRequest(name = "Bad Recipe", description = null, webLink = null, baseServings = 0, ingredients = emptyList()),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 404 when ingredient does not exist`() {
            val response = restTemplate.exchange(
                "/api/recipes",
                HttpMethod.POST,
                entityWithUser(
                    CreateRecipeRequest(
                        name = "Mystery Recipe",
                        description = null,
                        webLink = null,
                        baseServings = 2,
                        ingredients = listOf(
                            CreateRecipeIngredientRequest(
                                ingredientId = UUID.randomUUID(),
                                quantity = BigDecimal("1"),
                                unit = "pieces"
                            )
                        )
                    ),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class GetRecipe {

        @Test
        fun `GET returns 200 with recipe detail`() {
            val ingredientId = fixture.insertIngredient(name = "Olive Oil", category = "condiment", defaultUnit = "tbsp")
            val recipeId = fixture.insertRecipe(name = "Bruschetta", createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, ingredientId = ingredientId, quantity = BigDecimal("2"), unit = "tbsp")

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                RecipeDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.id).isEqualTo(recipeId)
            assertThat(response.body!!.name).isEqualTo("Bruschetta")
            assertThat(response.body!!.ingredients).hasSize(1)
            assertThat(response.body!!.ingredients[0].ingredient!!.name).isEqualTo("Olive Oil")
        }

        @Test
        fun `GET returns 404 when recipe not found`() {
            val response = restTemplate.exchange(
                "/api/recipes/${UUID.randomUUID()}",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class ListRecipes {

        @Test
        fun `GET returns 200 with empty list when no recipes`() {
            val response = restTemplate.exchange(
                "/api/recipes",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<RecipeResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).isEmpty()
        }

        @Test
        fun `GET returns published recipes and own drafts`() {
            fixture.insertRecipe(name = "My Published", status = "published", createdBy = userId)
            fixture.insertRecipe(name = "My Draft", status = "draft", createdBy = userId)
            fixture.insertRecipe(name = "Other Published", status = "published", createdBy = otherUserId)
            fixture.insertRecipe(name = "Other Draft", status = "draft", createdBy = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<RecipeResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val names = response.body!!.map { it.name }
            assertThat(names).containsExactlyInAnyOrder("My Published", "My Draft", "Other Published")
            assertThat(names).doesNotContain("Other Draft")
        }
    }

    @Nested
    inner class UpdateRecipe {

        @Test
        fun `PUT returns 200 when creator updates recipe`() {
            val recipeId = fixture.insertRecipe(name = "Old Name", createdBy = userId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId",
                HttpMethod.PUT,
                entityWithUser(UpdateRecipeRequest(name = "New Name", description = null, baseServings = null), userId),
                RecipeResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("New Name")
        }

        @Test
        fun `PUT returns 404 when recipe not found`() {
            val response = restTemplate.exchange(
                "/api/recipes/${UUID.randomUUID()}",
                HttpMethod.PUT,
                entityWithUser(UpdateRecipeRequest(name = "Nope", description = null, baseServings = null), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class DeleteRecipe {

        @Test
        fun `DELETE returns 204 when creator deletes recipe`() {
            val recipeId = fixture.insertRecipe(name = "To Delete", createdBy = userId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `DELETE returns 404 when recipe not found`() {
            val response = restTemplate.exchange(
                "/api/recipes/${UUID.randomUUID()}",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class ImportRecipe {

        @Test
        fun `POST returns 400 when URL is blank`() {
            val response = restTemplate.exchange(
                "/api/recipes/import",
                HttpMethod.POST,
                entityWithUser(ImportRecipeRequest(url = ""), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 409 when URL already imported`() {
            val webLink = "https://example.com/recipe/guacamole"
            fixture.insertRecipe(name = "Guacamole", webLink = webLink, createdBy = userId)

            val response = restTemplate.exchange(
                "/api/recipes/import",
                HttpMethod.POST,
                entityWithUser(ImportRecipeRequest(url = webLink), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @Nested
    inner class PublishRecipe {

        @Test
        fun `POST returns 200 when recipe is published`() {
            val recipeId = fixture.insertRecipe(name = "Ready Recipe", status = "draft", createdBy = userId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/publish",
                HttpMethod.POST,
                entityWithUser(null, userId),
                RecipeResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.status).isEqualTo("published")
        }

        @Test
        fun `POST returns 409 when recipe is already published`() {
            val recipeId = fixture.insertRecipe(name = "Already Published", status = "published", createdBy = userId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/publish",
                HttpMethod.POST,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        fun `POST returns 422 when recipe has pending ingredients`() {
            val recipeId = fixture.insertRecipe(name = "Needs Review", status = "draft", createdBy = userId)
            fixture.insertRecipeIngredient(recipeId = recipeId, status = "pending_review")

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/publish",
                HttpMethod.POST,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
        }

        @Test
        fun `POST returns 404 when recipe not found`() {
            val response = restTemplate.exchange(
                "/api/recipes/${UUID.randomUUID()}/publish",
                HttpMethod.POST,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class ResolveIngredient {

        @Test
        fun `PUT returns 200 when selecting existing ingredient`() {
            val ingredientId = fixture.insertIngredient(name = "Butter", category = "dairy", defaultUnit = "g")
            val recipeId = fixture.insertRecipe(name = "Croissant", status = "draft", createdBy = userId)
            val recipeIngredientId = fixture.insertRecipeIngredient(
                recipeId = recipeId,
                originalText = "2 tbsp butter",
                quantity = BigDecimal("2"),
                unit = "tbsp",
                status = "pending_review"
            )

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/ingredients/$recipeIngredientId",
                HttpMethod.PUT,
                entityWithUser(
                    ResolveIngredientRequest(action = "SELECT_EXISTING", ingredientId = ingredientId, newIngredient = null, quantity = null, unit = null),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        fun `PUT returns 200 when confirming matched ingredient`() {
            val ingredientId = fixture.insertIngredient(name = "Lemon", category = "produce", defaultUnit = "whole")
            val recipeId = fixture.insertRecipe(name = "Lemon Cake", status = "draft", createdBy = userId)
            val recipeIngredientId = fixture.insertRecipeIngredient(
                recipeId = recipeId,
                originalText = "1 lemon",
                quantity = BigDecimal("1"),
                unit = "whole",
                status = "pending_review",
                matchedIngredientId = ingredientId
            )

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/ingredients/$recipeIngredientId",
                HttpMethod.PUT,
                entityWithUser(
                    ResolveIngredientRequest(action = "CONFIRM_MATCH", ingredientId = null, newIngredient = null, quantity = null, unit = null),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        fun `PUT returns 200 when creating new ingredient`() {
            val recipeId = fixture.insertRecipe(name = "Exotic Dish", status = "draft", createdBy = userId)
            val recipeIngredientId = fixture.insertRecipeIngredient(
                recipeId = recipeId,
                originalText = "1 cup dragon fruit",
                quantity = BigDecimal("1"),
                unit = "cup",
                status = "pending_review"
            )

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/ingredients/$recipeIngredientId",
                HttpMethod.PUT,
                entityWithUser(
                    ResolveIngredientRequest(
                        action = "CREATE_NEW",
                        ingredientId = null,
                        newIngredient = CreateIngredientRequest(name = "Dragon Fruit", category = "produce", defaultUnit = "whole"),
                        quantity = null,
                        unit = null
                    ),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        fun `PUT returns 404 when recipe not found`() {
            val ingredientId = fixture.insertIngredient(name = "Pepper", category = "spice", defaultUnit = "pinch")

            val response = restTemplate.exchange(
                "/api/recipes/${UUID.randomUUID()}/ingredients/${UUID.randomUUID()}",
                HttpMethod.PUT,
                entityWithUser(
                    ResolveIngredientRequest(action = "SELECT_EXISTING", ingredientId = ingredientId, newIngredient = null, quantity = null, unit = null),
                    userId
                ),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    inner class ResolveDuplicate {

        @Test
        fun `PUT returns 200 with recipe when NOT_DUPLICATE`() {
            val originalId = fixture.insertRecipe(name = "Original Guacamole", status = "published", createdBy = otherUserId)
            val recipeId = fixture.insertRecipe(
                name = "Guacamole Copy",
                status = "draft",
                createdBy = userId,
                duplicateOfId = originalId
            )

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/resolve-duplicate",
                HttpMethod.PUT,
                entityWithUser(ResolveDuplicateRequest(action = "NOT_DUPLICATE"), userId),
                RecipeResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.duplicateOfId).isNull()
        }

        @Test
        fun `PUT returns 204 when USE_EXISTING deletes recipe`() {
            val originalId = fixture.insertRecipe(name = "Original Tacos", status = "published", createdBy = otherUserId)
            val recipeId = fixture.insertRecipe(
                name = "Tacos Copy",
                status = "draft",
                createdBy = userId,
                duplicateOfId = originalId
            )

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/resolve-duplicate",
                HttpMethod.PUT,
                entityWithUser(ResolveDuplicateRequest(action = "USE_EXISTING"), userId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @Test
        fun `PUT returns 404 when recipe not found`() {
            val response = restTemplate.exchange(
                "/api/recipes/${UUID.randomUUID()}/resolve-duplicate",
                HttpMethod.PUT,
                entityWithUser(ResolveDuplicateRequest(action = "NOT_DUPLICATE"), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
