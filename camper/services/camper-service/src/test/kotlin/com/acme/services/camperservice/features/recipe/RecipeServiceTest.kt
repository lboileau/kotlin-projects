package com.acme.services.camperservice.features.recipe

import com.acme.clients.common.Result
import com.acme.clients.ingredientclient.fake.FakeIngredientClient
import com.acme.clients.ingredientclient.model.Ingredient
import com.acme.clients.recipeclient.fake.FakeRecipeClient
import com.acme.clients.recipeclient.model.Recipe
import com.acme.clients.recipeclient.model.RecipeIngredient
import com.acme.clients.recipescraperclient.fake.FakeRecipeScraperClient
import com.acme.clients.recipescraperclient.model.ScrapedIngredient
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import com.acme.services.camperservice.features.recipe.actions.HtmlFetcher
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.params.*
import com.acme.services.camperservice.features.recipe.service.IngredientService
import com.acme.services.camperservice.features.recipe.service.RecipeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class RecipeServiceTest {

    private val fakeRecipeClient = FakeRecipeClient()
    private val fakeIngredientClient = FakeIngredientClient()
    private val fakeScraperClient = FakeRecipeScraperClient()
    private val fakeHtmlFetcher = HtmlFetcher { "<html>fake</html>" }

    private val recipeService = RecipeService(
        fakeRecipeClient, fakeIngredientClient, fakeScraperClient, fakeHtmlFetcher
    )
    private val ingredientService = IngredientService(fakeIngredientClient, fakeRecipeClient)

    private val userId = UUID.randomUUID()
    private val otherUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        fakeRecipeClient.reset()
        fakeIngredientClient.reset()
        fakeScraperClient.reset()
    }

    // ─── Ingredient helpers ───────────────────────────────────────────────

    private fun seedIngredient(name: String = "Tomato", category: String = "produce", unit: String = "g"): Ingredient {
        val ingredient = Ingredient(
            id = UUID.randomUUID(),
            name = name,
            category = category,
            defaultUnit = unit,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        fakeIngredientClient.seed(ingredient)
        return ingredient
    }

    private fun createIngredient(name: String, category: String = "produce", unit: String = "g"): Ingredient {
        val result = ingredientService.create(CreateIngredientParam(userId, name, category, unit))
        return (result as Result.Success).value.let { resp ->
            Ingredient(
                id = resp.id,
                name = resp.name,
                category = resp.category,
                defaultUnit = resp.defaultUnit,
                createdAt = resp.createdAt,
                updatedAt = resp.updatedAt
            )
        }
    }

    private fun createRecipe(
        name: String = "Test Recipe",
        baseServings: Int = 4,
        ingredients: List<CreateRecipeIngredientParam> = emptyList(),
        createdByUserId: UUID = userId
    ): com.acme.services.camperservice.features.recipe.dto.RecipeResponse {
        val result = recipeService.create(CreateRecipeParam(
            userId = createdByUserId,
            name = name,
            description = null,
            webLink = null,
            baseServings = baseServings,
            ingredients = ingredients
        ))
        return (result as Result.Success).value
    }

    // ─── Ingredient CRUD ──────────────────────────────────────────────────

    @Nested
    inner class CreateIngredient {

        @Test
        fun `create returns IngredientResponse on success`() {
            val result = ingredientService.create(CreateIngredientParam(userId, "Garlic", "produce", "clove"))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.name).isEqualTo("Garlic")
            assertThat(response.category).isEqualTo("produce")
            assertThat(response.defaultUnit).isEqualTo("clove")
            assertThat(response.id).isNotNull()
        }

        @Test
        fun `create returns Invalid when name is blank`() {
            val result = ingredientService.create(CreateIngredientParam(userId, "  ", "produce", "g"))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.Invalid::class.java)
        }

        @Test
        fun `create returns DuplicateIngredientName when name already exists`() {
            seedIngredient("Tomato")

            val result = ingredientService.create(CreateIngredientParam(userId, "Tomato", "produce", "g"))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.DuplicateIngredientName::class.java)
        }

        @Test
        fun `create is case-insensitive for duplicate check`() {
            seedIngredient("tomato")

            val result = ingredientService.create(CreateIngredientParam(userId, "TOMATO", "produce", "g"))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.DuplicateIngredientName::class.java)
        }
    }

    @Nested
    inner class ListIngredients {

        @Test
        fun `list returns all ingredients`() {
            seedIngredient("Avocado")
            seedIngredient("Tomato")

            val result = ingredientService.list(ListIngredientsParam(userId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).hasSize(2)
        }

        @Test
        fun `list returns empty list when no ingredients exist`() {
            val result = ingredientService.list(ListIngredientsParam(userId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    @Nested
    inner class UpdateIngredient {

        @Test
        fun `update returns updated IngredientResponse on success`() {
            val ingredient = seedIngredient("Old Name")

            val result = ingredientService.update(UpdateIngredientParam(
                ingredientId = ingredient.id,
                userId = userId,
                name = "New Name",
                category = null,
                defaultUnit = null
            ))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.name).isEqualTo("New Name")
        }

        @Test
        fun `update returns IngredientNotFound when ingredient does not exist`() {
            val result = ingredientService.update(UpdateIngredientParam(
                ingredientId = UUID.randomUUID(),
                userId = userId,
                name = "Nope",
                category = null,
                defaultUnit = null
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.IngredientNotFound::class.java)
        }
    }

    // ─── Recipe CRUD ──────────────────────────────────────────────────────

    @Nested
    inner class CreateRecipe {

        @Test
        fun `create returns RecipeResponse on success`() {
            val ingredient = seedIngredient()
            val result = recipeService.create(CreateRecipeParam(
                userId = userId,
                name = "Pasta",
                description = "Yummy pasta",
                webLink = null,
                baseServings = 2,
                ingredients = listOf(CreateRecipeIngredientParam(ingredient.id, BigDecimal("200"), "g"))
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response.name).isEqualTo("Pasta")
            assertThat(response.baseServings).isEqualTo(2)
            assertThat(response.status).isEqualTo("published")
            assertThat(response.createdBy).isEqualTo(userId)
        }

        @Test
        fun `create returns Invalid when name is blank`() {
            val result = recipeService.create(CreateRecipeParam(userId, "", null, null, 2, emptyList()))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.Invalid::class.java)
        }

        @Test
        fun `create returns Invalid when baseServings is zero`() {
            val result = recipeService.create(CreateRecipeParam(userId, "Soup", null, null, 0, emptyList()))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.Invalid::class.java)
        }

        @Test
        fun `create returns IngredientNotFound when ingredient ID does not exist`() {
            val result = recipeService.create(CreateRecipeParam(
                userId = userId,
                name = "Soup",
                description = null,
                webLink = null,
                baseServings = 2,
                ingredients = listOf(CreateRecipeIngredientParam(UUID.randomUUID(), BigDecimal("1"), "cup"))
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.IngredientNotFound::class.java)
        }

        @Test
        fun `create with no ingredients succeeds`() {
            val result = recipeService.create(CreateRecipeParam(userId, "Simple", null, null, 1, emptyList()))

            assertThat(result.isSuccess).isTrue()
        }
    }

    @Nested
    inner class GetRecipe {

        @Test
        fun `get returns RecipeDetailResponse with enriched ingredients`() {
            val ingredient = seedIngredient("Avocado", "produce", "whole")
            val recipe = createRecipe(
                ingredients = listOf(CreateRecipeIngredientParam(ingredient.id, BigDecimal("2"), "whole"))
            )

            val result = recipeService.get(GetRecipeParam(recipe.id, userId))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            assertThat(detail.id).isEqualTo(recipe.id)
            assertThat(detail.ingredients).hasSize(1)
            assertThat(detail.ingredients[0].ingredient?.name).isEqualTo("Avocado")
        }

        @Test
        fun `get returns NotFound when recipe does not exist`() {
            val result = recipeService.get(GetRecipeParam(UUID.randomUUID(), userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.NotFound::class.java)
        }

        @Test
        fun `get returns RecipeDetailResponse with null ingredient when pending review`() {
            val recipe = createRecipe()
            // Seed a recipe ingredient with no ingredientId (pending review)
            val recipeIngredient = RecipeIngredient(
                id = UUID.randomUUID(),
                recipeId = recipe.id,
                ingredientId = null,
                originalText = "2 cups something",
                quantity = BigDecimal("2"),
                unit = "cup",
                status = "pending_review",
                matchedIngredientId = null,
                suggestedIngredientName = "something",
                reviewFlags = listOf("NEW_INGREDIENT"),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            fakeRecipeClient.seedIngredients(recipeIngredient)

            val result = recipeService.get(GetRecipeParam(recipe.id, userId))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            assertThat(detail.ingredients).hasSize(1)
            assertThat(detail.ingredients[0].ingredient).isNull()
            assertThat(detail.ingredients[0].status).isEqualTo("pending_review")
        }
    }

    @Nested
    inner class ListRecipes {

        @Test
        fun `list returns published recipes for all users`() {
            val userA = UUID.randomUUID()
            val userB = UUID.randomUUID()
            createRecipe("Recipe A", createdByUserId = userA)
            createRecipe("Recipe B", createdByUserId = userB)

            val result = recipeService.list(ListRecipesParam(userId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).hasSize(2)
        }

        @Test
        fun `list includes own draft recipes`() {
            // Seed a draft recipe directly for userId
            val draftRecipe = Recipe(
                id = UUID.randomUUID(),
                name = "My Draft",
                description = null,
                webLink = null,
                baseServings = 2,
                status = "draft",
                createdBy = userId,
                duplicateOfId = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            fakeRecipeClient.seed(draftRecipe)

            val result = recipeService.list(ListRecipesParam(userId))

            assertThat(result.isSuccess).isTrue()
            val recipes = (result as Result.Success).value
            assertThat(recipes.map { it.name }).contains("My Draft")
        }

        @Test
        fun `list does not include other users drafts`() {
            val draftByOther = Recipe(
                id = UUID.randomUUID(),
                name = "Other Draft",
                description = null,
                webLink = null,
                baseServings = 2,
                status = "draft",
                createdBy = otherUserId,
                duplicateOfId = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            fakeRecipeClient.seed(draftByOther)

            val result = recipeService.list(ListRecipesParam(userId))

            assertThat(result.isSuccess).isTrue()
            val recipes = (result as Result.Success).value
            assertThat(recipes.map { it.name }).doesNotContain("Other Draft")
        }

        @Test
        fun `list returns empty when no recipes exist`() {
            val result = recipeService.list(ListRecipesParam(userId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).isEmpty()
        }
    }

    @Nested
    inner class UpdateRecipe {

        @Test
        fun `update returns updated RecipeResponse when creator updates`() {
            val recipe = createRecipe("Old Name")

            val result = recipeService.update(UpdateRecipeParam(
                recipeId = recipe.id,
                userId = userId,
                name = "New Name",
                description = "Updated description",
                baseServings = null
            ))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.name).isEqualTo("New Name")
        }

        @Test
        fun `update returns NotFound when recipe does not exist`() {
            val result = recipeService.update(UpdateRecipeParam(
                recipeId = UUID.randomUUID(),
                userId = userId,
                name = "Nope",
                description = null,
                baseServings = null
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.NotFound::class.java)
        }
    }

    @Nested
    inner class DeleteRecipe {

        @Test
        fun `delete returns success when creator deletes`() {
            val recipe = createRecipe()

            val result = recipeService.delete(DeleteRecipeParam(recipe.id, userId))

            assertThat(result.isSuccess).isTrue()
        }

        @Test
        fun `delete returns NotFound when recipe does not exist`() {
            val result = recipeService.delete(DeleteRecipeParam(UUID.randomUUID(), userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.NotFound::class.java)
        }
    }

    // ─── Import Flow ──────────────────────────────────────────────────────

    @Nested
    inner class ImportRecipe {

        @Test
        fun `import creates a draft recipe with pending_review ingredients from NoOp scraper`() {
            // Default FakeRecipeScraperClient returns canned guacamole recipe with NEW_INGREDIENT flags
            val result = recipeService.import(ImportRecipeParam(userId, "https://example.com/guacamole"))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            assertThat(detail.status).isEqualTo("draft")
            assertThat(detail.name).isEqualTo("Classic Guacamole")
            assertThat(detail.webLink).isEqualTo("https://example.com/guacamole")
            assertThat(detail.createdBy).isEqualTo(userId)
            assertThat(detail.ingredients).hasSize(1)
            assertThat(detail.ingredients[0].status).isEqualTo("pending_review")
            assertThat(detail.ingredients[0].reviewFlags).contains("NEW_INGREDIENT")
        }

        @Test
        fun `import returns DuplicateWebLink when URL already imported`() {
            // First import
            recipeService.import(ImportRecipeParam(userId, "https://example.com/guacamole"))

            // Second import of same URL
            val result = recipeService.import(ImportRecipeParam(userId, "https://example.com/guacamole"))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.DuplicateWebLink::class.java)
        }

        @Test
        fun `import returns Invalid when URL is blank`() {
            val result = recipeService.import(ImportRecipeParam(userId, ""))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.Invalid::class.java)
        }

        @Test
        fun `import returns ImportFailed when HTML fetch fails`() {
            val failingFetcher = HtmlFetcher { throw RuntimeException("Connection refused") }
            val serviceWithFailingFetcher = RecipeService(
                fakeRecipeClient, fakeIngredientClient, fakeScraperClient, failingFetcher
            )

            val result = serviceWithFailingFetcher.import(ImportRecipeParam(userId, "https://example.com/recipe"))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.ImportFailed::class.java)
        }

        @Test
        fun `import sets duplicate_of_id when similar recipe exists`() {
            // Create an existing recipe with a similar name
            val existingRecipe = Recipe(
                id = UUID.randomUUID(),
                name = "Classic Guacamole",
                description = null,
                webLink = null,
                baseServings = 4,
                status = "published",
                createdBy = otherUserId,
                duplicateOfId = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            fakeRecipeClient.seed(existingRecipe)

            val result = recipeService.import(ImportRecipeParam(userId, "https://example.com/guacamole"))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            // Should have a duplicateOf set since the scraper returns "Classic Guacamole"
            assertThat(detail.duplicateOf).isNotNull()
            assertThat(detail.duplicateOf?.id).isEqualTo(existingRecipe.id)
        }

        @Test
        fun `import sends existing ingredients to scraper`() {
            seedIngredient("avocado", "produce", "whole")

            recipeService.import(ImportRecipeParam(userId, "https://example.com/guacamole"))

            assertThat(fakeScraperClient.lastParam?.existingIngredients).hasSize(1)
            assertThat(fakeScraperClient.lastParam?.existingIngredients?.first()?.name).isEqualTo("avocado")
        }

        @Test
        fun `import ingredient with no review flags and matched ID gets approved status`() {
            val avocado = seedIngredient("avocado", "produce", "whole")

            fakeScraperClient.nextResult = Result.Success(ScrapedRecipe(
                name = "Guac",
                description = null,
                baseServings = 4,
                ingredients = listOf(
                    ScrapedIngredient(
                        originalText = "3 avocados",
                        quantity = BigDecimal("3"),
                        unit = "whole",
                        matchedIngredientId = avocado.id,
                        suggestedIngredientName = null,
                        confidence = "HIGH",
                        reviewFlags = emptyList()
                    )
                )
            ))

            val result = recipeService.import(ImportRecipeParam(userId, "https://example.com/guac"))

            assertThat(result.isSuccess).isTrue()
            val detail = (result as Result.Success).value
            assertThat(detail.ingredients[0].status).isEqualTo("approved")
            assertThat(detail.ingredients[0].ingredient?.id).isEqualTo(avocado.id)
        }
    }

    // ─── Review Flow ──────────────────────────────────────────────────────

    @Nested
    inner class ResolveIngredient {

        private fun setupDraftWithPendingIngredient(): Pair<UUID, UUID> {
            val importResult = recipeService.import(ImportRecipeParam(userId, "https://example.com/guacamole"))
            val detail = (importResult as Result.Success).value
            val recipeIngredientId = detail.ingredients[0].id
            return Pair(detail.id, recipeIngredientId)
        }

        @Test
        fun `CONFIRM_MATCH resolves ingredient using matched ingredient ID`() {
            val avocado = seedIngredient("avocado", "produce", "whole")
            fakeScraperClient.nextResult = Result.Success(ScrapedRecipe(
                name = "Guac",
                description = null,
                baseServings = 4,
                ingredients = listOf(
                    ScrapedIngredient(
                        originalText = "3 avocados",
                        quantity = BigDecimal("3"),
                        unit = "whole",
                        matchedIngredientId = avocado.id,
                        suggestedIngredientName = null,
                        confidence = "LOW",
                        reviewFlags = listOf("INGREDIENT_MATCH_UNCERTAIN")
                    )
                )
            ))
            val (recipeId, recipeIngredientId) = setupDraftWithPendingIngredient()

            val result = recipeService.resolveIngredient(ResolveIngredientParam(
                recipeId = recipeId,
                recipeIngredientId = recipeIngredientId,
                userId = userId,
                action = "CONFIRM_MATCH",
                ingredientId = null,
                newIngredient = null,
                quantity = null,
                unit = null
            ))

            assertThat(result.isSuccess).isTrue()
            val resolved = (result as Result.Success).value
            assertThat(resolved.status).isEqualTo("approved")
            assertThat(resolved.ingredient?.id).isEqualTo(avocado.id)
            assertThat(resolved.reviewFlags).isEmpty()
        }

        @Test
        fun `CREATE_NEW creates a new ingredient and links it`() {
            val (recipeId, recipeIngredientId) = setupDraftWithPendingIngredient()

            val result = recipeService.resolveIngredient(ResolveIngredientParam(
                recipeId = recipeId,
                recipeIngredientId = recipeIngredientId,
                userId = userId,
                action = "CREATE_NEW",
                ingredientId = null,
                newIngredient = com.acme.services.camperservice.features.recipe.dto.CreateIngredientRequest(
                    name = "avocado",
                    category = "produce",
                    defaultUnit = "whole"
                ),
                quantity = null,
                unit = null
            ))

            assertThat(result.isSuccess).isTrue()
            val resolved = (result as Result.Success).value
            assertThat(resolved.status).isEqualTo("approved")
            assertThat(resolved.ingredient?.name).isEqualTo("avocado")
        }

        @Test
        fun `SELECT_EXISTING links to an existing ingredient by ID`() {
            val avocado = seedIngredient("avocado")
            val (recipeId, recipeIngredientId) = setupDraftWithPendingIngredient()

            val result = recipeService.resolveIngredient(ResolveIngredientParam(
                recipeId = recipeId,
                recipeIngredientId = recipeIngredientId,
                userId = userId,
                action = "SELECT_EXISTING",
                ingredientId = avocado.id,
                newIngredient = null,
                quantity = null,
                unit = null
            ))

            assertThat(result.isSuccess).isTrue()
            val resolved = (result as Result.Success).value
            assertThat(resolved.status).isEqualTo("approved")
            assertThat(resolved.ingredient?.id).isEqualTo(avocado.id)
        }

        @Test
        fun `resolveIngredient returns NotFound when recipe does not exist`() {
            val result = recipeService.resolveIngredient(ResolveIngredientParam(
                recipeId = UUID.randomUUID(),
                recipeIngredientId = UUID.randomUUID(),
                userId = userId,
                action = "SELECT_EXISTING",
                ingredientId = UUID.randomUUID(),
                newIngredient = null,
                quantity = null,
                unit = null
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.NotFound::class.java)
        }

        @Test
        fun `resolveIngredient returns Invalid for unknown action`() {
            val (recipeId, recipeIngredientId) = setupDraftWithPendingIngredient()

            val result = recipeService.resolveIngredient(ResolveIngredientParam(
                recipeId = recipeId,
                recipeIngredientId = recipeIngredientId,
                userId = userId,
                action = "INVALID_ACTION",
                ingredientId = null,
                newIngredient = null,
                quantity = null,
                unit = null
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.Invalid::class.java)
        }
    }

    @Nested
    inner class ResolveDuplicate {

        private fun importGuacamole(url: String = "https://example.com/guacamole") =
            (recipeService.import(ImportRecipeParam(userId, url)) as Result.Success).value

        @Test
        fun `NOT_DUPLICATE clears the duplicate flag and returns RecipeResponse`() {
            val existingRecipe = Recipe(
                id = UUID.randomUUID(),
                name = "Classic Guacamole",
                description = null,
                webLink = null,
                baseServings = 4,
                status = "published",
                createdBy = otherUserId,
                duplicateOfId = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            fakeRecipeClient.seed(existingRecipe)
            val imported = importGuacamole()
            assertThat(imported.duplicateOf).isNotNull()

            val result = recipeService.resolveDuplicate(ResolveDuplicateParam(
                recipeId = imported.id,
                userId = userId,
                action = "NOT_DUPLICATE"
            ))

            assertThat(result.isSuccess).isTrue()
            val response = (result as Result.Success).value
            assertThat(response).isNotNull()
            assertThat(response?.duplicateOfId).isNull()
        }

        @Test
        fun `USE_EXISTING deletes the draft and returns null`() {
            val imported = importGuacamole()

            val result = recipeService.resolveDuplicate(ResolveDuplicateParam(
                recipeId = imported.id,
                userId = userId,
                action = "USE_EXISTING"
            ))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value).isNull()
        }

        @Test
        fun `resolveDuplicate returns NotFound when recipe does not exist`() {
            val result = recipeService.resolveDuplicate(ResolveDuplicateParam(
                recipeId = UUID.randomUUID(),
                userId = userId,
                action = "NOT_DUPLICATE"
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.NotFound::class.java)
        }

        @Test
        fun `resolveDuplicate returns Invalid for unknown action`() {
            val imported = importGuacamole()

            val result = recipeService.resolveDuplicate(ResolveDuplicateParam(
                recipeId = imported.id,
                userId = userId,
                action = "UNKNOWN"
            ))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.Invalid::class.java)
        }
    }

    @Nested
    inner class PublishRecipe {

        private fun importAndApproveAll(url: String = "https://example.com/guacamole"): UUID {
            val imported = (recipeService.import(ImportRecipeParam(userId, url)) as Result.Success).value

            // Approve each pending ingredient
            for (ri in imported.ingredients.filter { it.status == "pending_review" }) {
                val ingredient = seedIngredient(
                    name = ri.suggestedIngredientName ?: "ingredient-${ri.id}",
                    category = "produce",
                    unit = ri.unit
                )
                recipeService.resolveIngredient(ResolveIngredientParam(
                    recipeId = imported.id,
                    recipeIngredientId = ri.id,
                    userId = userId,
                    action = "SELECT_EXISTING",
                    ingredientId = ingredient.id,
                    newIngredient = null,
                    quantity = null,
                    unit = null
                ))
            }
            return imported.id
        }

        @Test
        fun `publish returns published RecipeResponse when all ingredients approved and no duplicate`() {
            val recipeId = importAndApproveAll()

            val result = recipeService.publish(PublishRecipeParam(recipeId, userId))

            assertThat(result.isSuccess).isTrue()
            assertThat((result as Result.Success).value.status).isEqualTo("published")
        }

        @Test
        fun `publish returns UnresolvedIngredients when some ingredients are pending`() {
            val imported = (recipeService.import(ImportRecipeParam(userId, "https://example.com/guacamole")) as Result.Success).value

            val result = recipeService.publish(PublishRecipeParam(imported.id, userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.UnresolvedIngredients::class.java)
        }

        @Test
        fun `publish returns UnresolvedDuplicate when duplicate not resolved`() {
            val existingRecipe = Recipe(
                id = UUID.randomUUID(),
                name = "Classic Guacamole",
                description = null,
                webLink = null,
                baseServings = 4,
                status = "published",
                createdBy = otherUserId,
                duplicateOfId = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            fakeRecipeClient.seed(existingRecipe)
            val recipeId = importAndApproveAll()

            val result = recipeService.publish(PublishRecipeParam(recipeId, userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.UnresolvedDuplicate::class.java)
        }

        @Test
        fun `publish returns AlreadyPublished when recipe is already published`() {
            val recipe = createRecipe()

            val result = recipeService.publish(PublishRecipeParam(recipe.id, userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.AlreadyPublished::class.java)
        }

        @Test
        fun `publish returns NotFound when recipe does not exist`() {
            val result = recipeService.publish(PublishRecipeParam(UUID.randomUUID(), userId))

            assertThat(result.isFailure).isTrue()
            assertThat((result as Result.Failure).error).isInstanceOf(RecipeError.NotFound::class.java)
        }
    }
}
