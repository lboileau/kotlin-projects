package com.acme.services.camperservice.features.recipe.acceptance

import com.acme.services.camperservice.config.TestContainerConfig
import com.acme.services.camperservice.features.recipe.acceptance.fixture.RecipeFixture
import com.acme.services.camperservice.features.recipe.dto.CreateIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.CreateRecipeIngredientRequest
import com.acme.services.camperservice.features.recipe.dto.CreateRecipeRequest
import com.acme.services.camperservice.features.recipe.dto.ImportImageRequest
import com.acme.services.camperservice.features.recipe.dto.ImportRecipeFromImagesRequest
import com.acme.services.camperservice.features.recipe.dto.ImportRecipeRequest
import com.acme.services.camperservice.features.recipe.dto.RecipeDetailResponse
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteStatusResponse
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteUserResponse
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
import java.time.Instant
import java.time.temporal.ChronoUnit
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
        fun `GET returns published recipes and the caller's own drafts, excluding other users drafts`() {
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
    inner class ImportRecipeFromImages {

        private fun photo(mediaType: String = "image/jpeg") =
            ImportImageRequest(mediaType, java.util.Base64.getEncoder().encodeToString(ByteArray(32) { 7 }))

        @Test
        fun `POST creates a draft with no web link from the stub scraper`() {
            // TestContainerConfig wires the NoOp scraper, so any photo yields the canned guacamole.
            val response = restTemplate.exchange(
                "/api/recipes/import-images",
                HttpMethod.POST,
                entityWithUser(ImportRecipeFromImagesRequest(listOf(photo(), photo("image/png"))), userId),
                RecipeDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            val detail = response.body!!
            assertThat(detail.status).isEqualTo("draft")
            assertThat(detail.name).isEqualTo("Classic Guacamole")
            assertThat(detail.webLink).isNull()
            assertThat(detail.createdBy).isEqualTo(userId)
            assertThat(detail.ingredients).hasSize(2)
            assertThat(detail.ingredients.map { it.status }).containsOnly("pending_review")
        }

        @Test
        fun `POST twice creates two drafts - photos have no web link to collide on`() {
            repeat(2) {
                val response = restTemplate.exchange(
                    "/api/recipes/import-images", HttpMethod.POST,
                    entityWithUser(ImportRecipeFromImagesRequest(listOf(photo())), userId),
                    RecipeDetailResponse::class.java
                )
                assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            }
        }

        @Test
        fun `POST returns 400 with no photos`() {
            val response = restTemplate.exchange(
                "/api/recipes/import-images", HttpMethod.POST,
                entityWithUser(ImportRecipeFromImagesRequest(emptyList()), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 400 with four photos`() {
            val response = restTemplate.exchange(
                "/api/recipes/import-images", HttpMethod.POST,
                entityWithUser(ImportRecipeFromImagesRequest(List(4) { photo() }), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `POST returns 400 for an unsupported media type`() {
            val response = restTemplate.exchange(
                "/api/recipes/import-images", HttpMethod.POST,
                entityWithUser(ImportRecipeFromImagesRequest(listOf(photo("application/pdf"))), userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body!!["message"].toString()).contains("images[0].mediaType")
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

    @Nested
    inner class Favorites {

        @Test
        fun `PUT favorite returns 200 with count 1 and favoritedByMe true`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite",
                HttpMethod.PUT,
                entityWithUser(null, userId),
                RecipeFavoriteStatusResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.recipeId).isEqualTo(recipeId)
            assertThat(response.body!!.favoriteCount).isEqualTo(1)
            assertThat(response.body!!.favoritedByMe).isTrue()

            val rowCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM recipe_favorites WHERE recipe_id = ? AND user_id = ?",
                Int::class.java, recipeId, userId
            )
            assertThat(rowCount).isEqualTo(1)
        }

        @Test
        fun `PUT favorite twice returns 200 and count stays 1`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)

            restTemplate.exchange(
                "/api/recipes/$recipeId/favorite", HttpMethod.PUT, entityWithUser(null, userId), RecipeFavoriteStatusResponse::class.java
            )
            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite", HttpMethod.PUT, entityWithUser(null, userId), RecipeFavoriteStatusResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.favoriteCount).isEqualTo(1)
            assertThat(response.body!!.favoritedByMe).isTrue()

            val rowCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM recipe_favorites WHERE recipe_id = ? AND user_id = ?",
                Int::class.java, recipeId, userId
            )
            assertThat(rowCount).isEqualTo(1)
        }

        @Test
        fun `DELETE favorite returns 200 with count 0 and favoritedByMe false`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)
            fixture.insertFavorite(recipeId = recipeId, userId = userId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                RecipeFavoriteStatusResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.favoriteCount).isEqualTo(0)
            assertThat(response.body!!.favoritedByMe).isFalse()
        }

        @Test
        fun `DELETE favorite twice returns 200 both times and leaves the other user's favourite`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)
            fixture.insertFavorite(recipeId = recipeId, userId = userId)
            fixture.insertFavorite(recipeId = recipeId, userId = otherUserId)
            restTemplate.exchange(
                "/api/recipes/$recipeId/favorite", HttpMethod.DELETE, entityWithUser(null, userId), RecipeFavoriteStatusResponse::class.java
            )

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                RecipeFavoriteStatusResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.favoriteCount).isEqualTo(1)
            assertThat(response.body!!.favoritedByMe).isFalse()
        }

        @Test
        fun `DELETE favorite when never favourited returns 200 with count 0`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                RecipeFavoriteStatusResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.favoriteCount).isEqualTo(0)
            assertThat(response.body!!.favoritedByMe).isFalse()
        }

        @Test
        fun `DELETE favorite does not remove another user's favourite`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)
            fixture.insertFavorite(recipeId = recipeId, userId = userId)
            fixture.insertFavorite(recipeId = recipeId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                RecipeFavoriteStatusResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.favoriteCount).isEqualTo(1)

            val remaining = jdbcTemplate.queryForObject(
                "SELECT user_id FROM recipe_favorites WHERE recipe_id = ?",
                UUID::class.java, recipeId
            )
            assertThat(remaining).isEqualTo(otherUserId)
        }

        @Test
        fun `PUT favorite returns 404 for an unknown recipe id`() {
            val response = restTemplate.exchange(
                "/api/recipes/${UUID.randomUUID()}/favorite",
                HttpMethod.PUT,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `DELETE favorite returns 404 for an unknown recipe id`() {
            val response = restTemplate.exchange(
                "/api/recipes/${UUID.randomUUID()}/favorite",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `GET favorites returns 404 for an unknown recipe id`() {
            val response = restTemplate.exchange(
                "/api/recipes/${UUID.randomUUID()}/favorites",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `PUT favorite returns 404 for another user's draft`() {
            val recipeId = fixture.insertRecipe(name = "Secret Recipe", status = "draft", createdBy = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite",
                HttpMethod.PUT,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `DELETE favorite returns 404 for another user's draft`() {
            val recipeId = fixture.insertRecipe(name = "Secret Recipe", status = "draft", createdBy = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `GET favorites returns 404 for another user's draft`() {
            val recipeId = fixture.insertRecipe(name = "Secret Recipe", status = "draft", createdBy = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorites",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `PUT favorite returns 200 on the caller's own draft`() {
            val recipeId = fixture.insertRecipe(name = "My Draft", status = "draft", createdBy = userId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite",
                HttpMethod.PUT,
                entityWithUser(null, userId),
                RecipeFavoriteStatusResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.favoriteCount).isEqualTo(1)
            assertThat(response.body!!.favoritedByMe).isTrue()
        }

        @Test
        fun `GET favorites returns people oldest first`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)
            val thirdUserId = fixture.insertUser(email = "third@example.com", username = "third")
            val now = Instant.now()
            fixture.insertFavorite(recipeId = recipeId, userId = otherUserId, createdAt = now.minus(2, ChronoUnit.HOURS))
            fixture.insertFavorite(recipeId = recipeId, userId = userId, createdAt = now.minus(1, ChronoUnit.HOURS))
            fixture.insertFavorite(recipeId = recipeId, userId = thirdUserId, createdAt = now)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorites",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<RecipeFavoriteUserResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.map { it.userId }).containsExactly(otherUserId, userId, thirdUserId)
        }

        @Test
        fun `GET favorites returns the email when the user has no username`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)
            val noUsernameId = fixture.insertUser(email = "no-username@example.com", username = null)
            fixture.insertFavorite(recipeId = recipeId, userId = noUsernameId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorites",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<RecipeFavoriteUserResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).hasSize(1)
            assertThat(response.body!![0].userId).isEqualTo(noUsernameId)
            assertThat(response.body!![0].username).isEqualTo("no-username@example.com")
            assertThat(response.body!![0].favoritedAt).isNotNull()
        }

        @Test
        fun `GET favorites returns an empty list when nobody has favourited`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorites",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<RecipeFavoriteUserResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body).isEmpty()
        }

        @Test
        fun `GET recipes returns favoriteCount and favoritedByMe per recipe`() {
            val favouritedId = fixture.insertRecipe(name = "Favourited", createdBy = otherUserId)
            val unfavouritedId = fixture.insertRecipe(name = "Unfavourited", createdBy = otherUserId)
            fixture.insertFavorite(recipeId = favouritedId, userId = userId)
            fixture.insertFavorite(recipeId = favouritedId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes",
                HttpMethod.GET,
                entityWithUser(null, userId),
                Array<RecipeResponse>::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val byId = response.body!!.associateBy { it.id }
            assertThat(byId[favouritedId]!!.favoriteCount).isEqualTo(2)
            assertThat(byId[favouritedId]!!.favoritedByMe).isTrue()
            assertThat(byId[unfavouritedId]!!.favoriteCount).isEqualTo(0)
            assertThat(byId[unfavouritedId]!!.favoritedByMe).isFalse()
        }

        @Test
        fun `GET recipes reports favoritedByMe differently for two callers`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)
            fixture.insertFavorite(recipeId = recipeId, userId = userId)

            val asFavouriter = restTemplate.exchange(
                "/api/recipes", HttpMethod.GET, entityWithUser(null, userId), Array<RecipeResponse>::class.java
            )
            val asOther = restTemplate.exchange(
                "/api/recipes", HttpMethod.GET, entityWithUser(null, otherUserId), Array<RecipeResponse>::class.java
            )

            val favouriterView = asFavouriter.body!!.first { it.id == recipeId }
            val otherView = asOther.body!!.first { it.id == recipeId }
            assertThat(favouriterView.favoriteCount).isEqualTo(1)
            assertThat(favouriterView.favoritedByMe).isTrue()
            assertThat(otherView.favoriteCount).isEqualTo(1)
            assertThat(otherView.favoritedByMe).isFalse()
        }

        @Test
        fun `GET recipe detail returns favoriteCount and favoritedByMe`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)
            fixture.insertFavorite(recipeId = recipeId, userId = userId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId",
                HttpMethod.GET,
                entityWithUser(null, userId),
                RecipeDetailResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.favoriteCount).isEqualTo(1)
            assertThat(response.body!!.favoritedByMe).isTrue()
        }

        @Test
        fun `PUT recipe returns the current favourite count`() {
            val recipeId = fixture.insertRecipe(name = "Old Name", createdBy = userId)
            // Favourited by someone else AND by the editor, so neither field's
            // real value (2, true) is what a hardcoded default (0, false) would be.
            fixture.insertFavorite(recipeId = recipeId, userId = otherUserId)
            fixture.insertFavorite(recipeId = recipeId, userId = userId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId",
                HttpMethod.PUT,
                entityWithUser(UpdateRecipeRequest(name = "New Name", description = null, baseServings = null), userId),
                RecipeResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.name).isEqualTo("New Name")
            assertThat(response.body!!.favoriteCount).isEqualTo(2)
            assertThat(response.body!!.favoritedByMe).isTrue()
        }

        @Test
        fun `POST publish returns the current favourite count`() {
            val recipeId = fixture.insertRecipe(name = "Ready Recipe", status = "draft", createdBy = userId)
            // Only the creator can see/favourite their own draft.
            val favoriteResponse = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite", HttpMethod.PUT, entityWithUser(null, userId), RecipeFavoriteStatusResponse::class.java
            )
            assertThat(favoriteResponse.statusCode).isEqualTo(HttpStatus.OK)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/publish",
                HttpMethod.POST,
                entityWithUser(null, userId),
                RecipeResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.status).isEqualTo("published")
            assertThat(response.body!!.favoriteCount).isEqualTo(1)
            // The publisher is the one who favourited it: a hardcoded default would say false.
            assertThat(response.body!!.favoritedByMe).isTrue()
        }

        @Test
        fun `POST recipe returns favoriteCount 0 and favoritedByMe false`() {
            val response = restTemplate.exchange(
                "/api/recipes",
                HttpMethod.POST,
                entityWithUser(
                    CreateRecipeRequest(name = "Fresh Recipe", description = null, webLink = null, baseServings = 2, ingredients = emptyList()),
                    userId
                ),
                RecipeResponse::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body!!.favoriteCount).isEqualTo(0)
            assertThat(response.body!!.favoritedByMe).isFalse()
        }

        @Test
        fun `DELETE recipe removes its favourites`() {
            val recipeId = fixture.insertRecipe(name = "To Delete", createdBy = userId)
            fixture.insertFavorite(recipeId = recipeId, userId = userId)
            fixture.insertFavorite(recipeId = recipeId, userId = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId",
                HttpMethod.DELETE,
                entityWithUser(null, userId),
                Void::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            val remaining = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM recipe_favorites WHERE recipe_id = ?",
                Int::class.java, recipeId
            )
            assertThat(remaining).isEqualTo(0)
        }

        @Test
        fun `deleting a user cascades their favourites away`() {
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)
            fixture.insertFavorite(recipeId = recipeId, userId = userId)

            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId)

            val remaining = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM recipe_favorites WHERE recipe_id = ?",
                Int::class.java, recipeId
            )
            assertThat(remaining).isEqualTo(0)
        }

        @Test
        fun `PUT favorite without X-User-Id header returns 400`() {
            val headers = HttpHeaders()
            val recipeId = fixture.insertRecipe(name = "Chili", createdBy = otherUserId)

            val response = restTemplate.exchange(
                "/api/recipes/$recipeId/favorite",
                HttpMethod.PUT,
                HttpEntity<Any?>(null, headers),
                Map::class.java
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    private fun entityWithUser(body: Any?, userId: UUID): HttpEntity<Any?> {
        val headers = HttpHeaders()
        headers.set("X-User-Id", userId.toString())
        return HttpEntity(body, headers)
    }
}
