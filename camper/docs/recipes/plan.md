# Feature: Recipes & Ingredients

## Summary

Add a global recipe catalog with ingredient normalization. Users can create recipes manually or import them from any URL using the Claude API to scrape, parse, and normalize ingredients. Imported recipes go through a review workflow (DRAFT → PUBLISHED) where users resolve ingredient matches, approve new ingredients, and handle unit normalization before the recipe becomes publicly available. A global ingredients table serves as the master reference for normalization across all recipes.

## Entities

### Ingredient (Global Master)

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | PK |
| name | VARCHAR(255) | Required, unique, normalized (lowercase, singular) |
| category | VARCHAR(50) | produce, dairy, meat, seafood, pantry, spice, condiment, frozen, bakery, other |
| default_unit | VARCHAR(20) | Canonical unit for this ingredient (g, ml, pieces, etc.) |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

Constraints:
- `UNIQUE(name)` — no duplicate ingredient names
- `CHECK(category IN ('produce', 'dairy', 'meat', 'seafood', 'pantry', 'spice', 'condiment', 'frozen', 'bakery', 'other'))`
- `CHECK(default_unit IN ('g', 'kg', 'ml', 'l', 'tsp', 'tbsp', 'cup', 'oz', 'lb', 'pieces', 'whole', 'bunch', 'can', 'clove', 'pinch', 'slice', 'sprig'))`

### Recipe

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | PK |
| name | VARCHAR(255) | Required, non-blank |
| description | TEXT | Optional |
| web_link | TEXT | Source URL, unique when not null |
| base_servings | INT | Number of people the recipe serves as written |
| status | VARCHAR(20) | 'draft' or 'published' |
| created_by | UUID | FK → users |
| duplicate_of_id | UUID | FK → recipes, nullable — flagged potential duplicate |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

Constraints:
- `UNIQUE(web_link)` where web_link IS NOT NULL — no duplicate URL imports
- `CHECK(status IN ('draft', 'published'))`
- `CHECK(base_servings > 0)`

### Recipe Ingredient

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | PK |
| recipe_id | UUID | FK → recipes, ON DELETE CASCADE |
| ingredient_id | UUID | FK → ingredients, nullable (null while pending review) |
| original_text | TEXT | Raw scraped text (e.g., "2 cups ripe red tomatoes"), nullable for manual |
| quantity | NUMERIC | Amount in the specified unit |
| unit | VARCHAR(20) | Unit of measurement |
| status | VARCHAR(20) | 'pending_review' or 'approved', default 'approved' |
| matched_ingredient_id | UUID | FK → ingredients, nullable — Claude's suggested match |
| suggested_ingredient_name | TEXT | Normalized name suggestion for new ingredients, nullable |
| review_flags | JSONB | Array of flags needing resolution, default '[]' |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

Constraints:
- `CHECK(status IN ('pending_review', 'approved'))`
- `CHECK(quantity > 0)`
- When status = 'approved', ingredient_id must NOT be null
- `UNIQUE(recipe_id, ingredient_id)` where ingredient_id IS NOT NULL

Review flag types (stored in JSONB array):
- `INGREDIENT_MATCH_UNCERTAIN` — Claude found a possible match but isn't confident
- `NEW_INGREDIENT` — No match found, suggesting a new ingredient be created
- `UNIT_CONVERSION_NEEDED` — Unit doesn't match ingredient's default_unit

## API Surface

### Ingredient Endpoints

Base path: `/api/ingredients`

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|-------------|----------|
| GET | `/ingredients` | List all global ingredients | — | 200 `List<IngredientResponse>` |
| POST | `/ingredients` | Create new ingredient | `CreateIngredientRequest` | 201 `IngredientResponse` |
| PUT | `/ingredients/{id}` | Update ingredient | `UpdateIngredientRequest` | 200 `IngredientResponse` |

### Recipe Endpoints

Base path: `/api/recipes`

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|-------------|----------|
| POST | `/recipes` | Create recipe manually | `CreateRecipeRequest` | 201 `RecipeResponse` |
| POST | `/recipes/import` | Import recipe from URL | `ImportRecipeRequest` | 201 `RecipeDetailResponse` (draft with review flags) |
| GET | `/recipes` | List published recipes (+ own drafts) | — | 200 `List<RecipeResponse>` |
| GET | `/recipes/{id}` | Get recipe with ingredients | — | 200 `RecipeDetailResponse` |
| PUT | `/recipes/{id}` | Update recipe metadata | `UpdateRecipeRequest` | 200 `RecipeResponse` |
| DELETE | `/recipes/{id}` | Delete recipe (creator only) | — | 204 |
| PUT | `/recipes/{id}/ingredients/{ingredientId}` | Resolve ingredient review | `ResolveIngredientRequest` | 200 `RecipeIngredientResponse` |
| PUT | `/recipes/{id}/resolve-duplicate` | Resolve duplicate flag | `ResolveDuplicateRequest` | 200 `RecipeResponse` or 204 (if deleted) |
| POST | `/recipes/{id}/publish` | Publish recipe | — | 200 `RecipeResponse` |

### Request/Response Shapes

```
CreateIngredientRequest { name: String, category: String, defaultUnit: String }
UpdateIngredientRequest { name: String?, category: String?, defaultUnit: String? }

IngredientResponse { id, name, category, defaultUnit, createdAt, updatedAt }

CreateRecipeRequest {
    name: String,
    description: String?,
    webLink: String?,
    baseServings: Int,
    ingredients: List<CreateRecipeIngredientRequest>
}
CreateRecipeIngredientRequest { ingredientId: UUID, quantity: BigDecimal, unit: String }

ImportRecipeRequest { url: String }

UpdateRecipeRequest { name: String?, description: String?, baseServings: Int? }

RecipeResponse { id, name, description, webLink, baseServings, status, createdBy, duplicateOfId, createdAt, updatedAt }
RecipeDetailResponse {
    id, name, description, webLink, baseServings, status, createdBy,
    duplicateOf: RecipeResponse?,  // the potential duplicate recipe, if flagged
    ingredients: List<RecipeIngredientResponse>,
    createdAt, updatedAt
}
RecipeIngredientResponse {
    id, recipeId,
    ingredient: IngredientResponse?,  // null if pending review
    originalText: String?,
    quantity: BigDecimal, unit: String,
    status: String,
    matchedIngredient: IngredientResponse?,  // Claude's suggestion
    suggestedIngredientName: String?,
    reviewFlags: List<String>,
    createdAt, updatedAt
}

ResolveIngredientRequest {
    action: String,  // "CONFIRM_MATCH", "CREATE_NEW", "SELECT_EXISTING"
    ingredientId: UUID?,  // for SELECT_EXISTING
    newIngredient: CreateIngredientRequest?  // for CREATE_NEW
}

ResolveDuplicateRequest {
    action: String  // "NOT_DUPLICATE" or "USE_EXISTING"
}
```

### Authorization Rules

| Action | Who |
|--------|-----|
| List/view published recipes | Any user |
| View draft recipe | Creator only |
| Create recipe (manual) | Any user |
| Import recipe (URL) | Any user |
| Edit/delete recipe | Creator only |
| Resolve ingredients / duplicate | Creator only |
| Publish recipe | Creator only |
| List/create/edit ingredients | Any user |

### Validation Rules

- Recipe name must not be blank
- base_servings must be > 0
- web_link must be unique (no duplicate imports)
- Ingredient name must not be blank and must be unique (case-insensitive)
- Ingredient category must be from allowed set
- Ingredient unit must be from allowed set
- quantity must be > 0
- Cannot publish recipe with unresolved ingredients (any status = 'pending_review')
- Cannot publish recipe with unresolved duplicate flag
- On import: check existing recipes for similar names/descriptions and flag potential duplicates

## Database Changes

- New table: `ingredients`
- New table: `recipes`
- New table: `recipe_ingredients`
- Three migrations: V013, V014, V015

## Client Interfaces

### ingredient-client

New client: `ingredient-client` in `clients/ingredient-client/`

```kotlin
interface IngredientClient {
    fun create(param: CreateIngredientParam): Result<Ingredient, AppError>
    fun getById(param: GetByIdParam): Result<Ingredient, AppError>
    fun getAll(): Result<List<Ingredient>, AppError>
    fun update(param: UpdateIngredientParam): Result<Ingredient, AppError>
    fun findByName(param: FindByNameParam): Result<Ingredient?, AppError>
    fun findByNames(param: FindByNamesParam): Result<List<Ingredient>, AppError>
    fun createBatch(param: CreateBatchParam): Result<List<Ingredient>, AppError>
}
```

### recipe-client

New client: `recipe-client` in `clients/recipe-client/`

```kotlin
interface RecipeClient {
    fun create(param: CreateRecipeParam): Result<Recipe, AppError>
    fun getById(param: GetByIdParam): Result<Recipe, AppError>
    fun getAll(param: GetAllParam): Result<List<Recipe>, AppError>
    fun update(param: UpdateRecipeParam): Result<Recipe, AppError>
    fun delete(param: DeleteRecipeParam): Result<Unit, AppError>
    fun findByWebLink(param: FindByWebLinkParam): Result<Recipe?, AppError>
    fun findSimilarByName(param: FindSimilarParam): Result<List<Recipe>, AppError>

    // Recipe ingredients
    fun addIngredient(param: AddRecipeIngredientParam): Result<RecipeIngredient, AppError>
    fun addIngredients(param: AddRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError>
    fun getIngredients(param: GetRecipeIngredientsParam): Result<List<RecipeIngredient>, AppError>
    fun updateIngredient(param: UpdateRecipeIngredientParam): Result<RecipeIngredient, AppError>
    fun removeIngredient(param: RemoveRecipeIngredientParam): Result<Unit, AppError>
}
```

### recipe-scraper-client

New client: `recipe-scraper-client` in `clients/recipe-scraper-client/`

External API client (similar to email-client pattern). No DB access.

```kotlin
interface RecipeScraperClient {
    fun scrape(param: ScrapeRecipeParam): Result<ScrapedRecipe, AppError>
}
```

**ScrapeRecipeParam:**
```kotlin
data class ScrapeRecipeParam(
    val html: String,
    val sourceUrl: String,
    val existingIngredients: List<ExistingIngredient>
)
data class ExistingIngredient(val id: UUID, val name: String, val category: String, val defaultUnit: String)
```

**ScrapedRecipe (returned by Claude):**
```kotlin
data class ScrapedRecipe(
    val name: String,
    val description: String?,
    val baseServings: Int,
    val ingredients: List<ScrapedIngredient>
)
data class ScrapedIngredient(
    val originalText: String,
    val quantity: BigDecimal,
    val unit: String,
    val matchedIngredientId: UUID?,
    val suggestedIngredientName: String?,
    val confidence: String,  // "HIGH" or "LOW"
    val reviewFlags: List<String>
)
```

The scraper client:
1. Receives HTML + list of existing global ingredients
2. Sends a structured prompt to Claude API asking it to:
   - Extract recipe name, description, servings
   - Extract each ingredient with quantity and unit
   - Match ingredients against the provided global list (by name similarity)
   - Normalize units to the allowed set
   - Flag uncertain matches, new ingredients, and unit conversion issues
3. Returns the structured result

**Implementations:**
- `AnthropicRecipeScraperClient` — calls Claude API via Anthropic SDK
- `NoOpRecipeScraperClient` — returns a canned response (for local dev without API key)

## Service Layer

New feature: `recipe` in `services/camper-service/src/main/kotlin/.../features/recipe/`

### Actions

| Action | Validates | Calls |
|--------|-----------|-------|
| CreateIngredient | name not blank, unique, valid category/unit | ingredientClient.create |
| ListIngredients | — | ingredientClient.getAll |
| UpdateIngredient | requester exists, valid fields | ingredientClient.update |
| CreateRecipe | name not blank, servings > 0, all ingredient IDs valid | recipeClient.create + addIngredients |
| ImportRecipe | URL not blank, not already imported | Fetches HTML → recipeScraperClient.scrape → recipeClient.create (as draft) + addIngredients (as pending_review) → check for duplicate recipes |
| GetRecipe | — | recipeClient.getById + getIngredients, enriches with ingredient details |
| ListRecipes | — | recipeClient.getAll (published + own drafts) |
| UpdateRecipe | requester is creator | recipeClient.update |
| DeleteRecipe | requester is creator | recipeClient.delete |
| ResolveIngredient | requester is creator, valid action | Creates new ingredient if needed → recipeClient.updateIngredient |
| ResolveDuplicate | requester is creator | Clears flag or deletes draft |
| PublishRecipe | requester is creator, all ingredients approved, duplicate resolved | recipeClient.update (status → published) |

### Error Types

```kotlin
sealed class RecipeError : AppError {
    NotFound(recipeId)
    NotCreator(recipeId, userId)
    Invalid(field, reason)
    DuplicateWebLink(url)
    DuplicateIngredientName(name)
    UnresolvedIngredients(recipeId, count)
    UnresolvedDuplicate(recipeId)
    ImportFailed(url, reason)
    ScrapeFailed(reason)
    IngredientNotFound(ingredientId)
    AlreadyPublished(recipeId)
}
```

## Import Flow (detailed)

```
1. User: POST /api/recipes/import { url: "https://example.com/guacamole" }

2. Service: ImportRecipeAction
   a. Check URL not already imported (findByWebLink)
   b. Fetch HTML from URL (HTTP GET)
   c. Load all global ingredients (ingredientClient.getAll)
   d. Call recipeScraperClient.scrape(html, url, existingIngredients)
   e. Claude returns structured ScrapedRecipe:
      - name: "Classic Guacamole"
      - baseServings: 4
      - ingredients:
        - { originalText: "3 ripe avocados", quantity: 3, unit: "whole",
            matchedIngredientId: <avocado-uuid>, confidence: "HIGH", flags: [] }
        - { originalText: "1/2 cup finely diced red onion", quantity: 0.5, unit: "cup",
            matchedIngredientId: <onion-uuid>, confidence: "LOW",
            flags: ["INGREDIENT_MATCH_UNCERTAIN"] }
        - { originalText: "2 tbsp fresh cilantro", quantity: 2, unit: "tbsp",
            matchedIngredientId: null, suggestedName: "cilantro",
            confidence: "LOW", flags: ["NEW_INGREDIENT"] }
   f. Check for similar existing recipes (findSimilarByName "Classic Guacamole")
      - If match found: set duplicate_of_id on new recipe
   g. Create draft recipe + recipe_ingredients (with review state)
   h. Return RecipeDetailResponse with all flags

3. User reviews flagged ingredients:
   a. PUT /recipes/{id}/ingredients/{id}/resolve
      { action: "CONFIRM_MATCH" }  → sets ingredient_id, status = approved
   b. PUT /recipes/{id}/ingredients/{id}/resolve
      { action: "CREATE_NEW", newIngredient: { name: "cilantro", category: "produce", defaultUnit: "tbsp" } }
      → creates ingredient in global table, links it, status = approved
   c. PUT /recipes/{id}/ingredients/{id}/resolve
      { action: "SELECT_EXISTING", ingredientId: <some-uuid> }
      → links to chosen ingredient, status = approved

4. User resolves duplicate (if flagged):
   PUT /recipes/{id}/resolve-duplicate { action: "NOT_DUPLICATE" }

5. User publishes:
   POST /recipes/{id}/publish
   → validates all ingredients approved, no unresolved duplicate → status = published
```

## PR Stack

| # | Branch | Title | Description |
|---|--------|-------|-------------|
| 1 | plan | feat(recipes): plan | This document |
| 2 | db-contracts | feat(recipes): db contracts | Schema files + migrations for ingredients, recipes, recipe_ingredients tables |
| 3 | client-contracts | feat(recipes): client contracts | ingredient-client + recipe-client interfaces, params, models, fake stubs |
| 4 | scraper-contracts | feat(recipes): scraper client contracts | recipe-scraper-client interface, params, models |
| 5 | service-contracts | feat(recipes): service contracts | DTOs, errors, action signatures, routes (501s) |
| 6 | db-impl | feat(recipes): db implementation | Seed data for common ingredients |
| 7 | client-impl | feat(recipes): client implementation | ingredient-client + recipe-client JDBI operations, factories, fakes |
| 8 | scraper-impl | feat(recipes): scraper client implementation | Claude API integration via Anthropic SDK, NoOp fallback |
| 9 | service-impl | feat(recipes): service implementation | Actions, services, controllers, HTML fetching, wiring |
| 10 | client-tests | feat(recipes): client tests | Integration tests for ingredient-client + recipe-client |
| 11 | service-tests | feat(recipes): service tests | Unit tests with fake clients |
| 12 | acceptance | feat(recipes): acceptance tests | End-to-end API tests |
| 13 | docs | feat(recipes): documentation | Update CLAUDE.md, README, skills |

## Open Questions

None — all requirements clarified.
