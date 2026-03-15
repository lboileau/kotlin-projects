# camper-service

API service for camping trip planning — user registration, authentication, plan management, assignment management, and recipe management.

## Package
`com.acme.services.camperservice`

## Architecture
- Spring Boot 3.4.3 application on port 8080
- Consumes `world-client`, `user-client`, `plan-client`, `item-client`, `itinerary-client`, `assignment-client`, `invitation-client`, `email-client`, `ingredient-client`, `recipe-client`, `recipe-scraper-client`, and `meal-plan-client` for data access
- Uses `meal-plan-calculator` lib for shopping list computation and unit conversion
- Uses `avatar-generator` lib for deterministic avatar generation from seed strings
- Database: `camper-db` (port 5433, database `camper_db`)
- **WebSocket:** STOMP-over-WebSocket at `/ws` for live updates. `PlanEventPublisher` broadcasts `PlanUpdateMessage(resource, action)` to `/topic/plans/{planId}` after successful mutations.

## Features

### World (`features/world/`)
- **Model:** `World(id, name, greeting, createdAt, updatedAt)`
- **DTOs:** `CreateWorldRequest(name, greeting)`, `UpdateWorldRequest(name?, greeting?)`, `WorldResponse(id, name, greeting, createdAt, updatedAt)`
- **Error:** `WorldError` sealed class — `NotFound(entityId)`, `AlreadyExists(name)`, `Invalid(field, reason)`
- **Service params:** `GetWorldByIdParam(id)`, `GetAllWorldsParam(limit?, offset?)`, `CreateWorldParam(name, greeting)`, `UpdateWorldParam(id, name?, greeting?)`, `DeleteWorldParam(id)`
- **Validations:** 1:1 with actions in `validations/`
  - `ValidateCreateWorld`: name must not be blank, greeting must not be blank
  - `ValidateUpdateWorld`: name must not be blank (if provided), greeting must not be blank (if provided)
  - `ValidateGetWorldById`, `ValidateGetAllWorlds`, `ValidateDeleteWorld`: default (return `success(Unit)`)
- **Actions:** 1:1 with service methods in `actions/`
  - `GetWorldByIdAction`, `GetAllWorldsAction`, `CreateWorldAction`, `UpdateWorldAction`, `DeleteWorldAction`
- **Service:** `WorldService` facade (no `@Service`, wired via `@Configuration` bean)
- **Routes:**
  - `GET /api/worlds/{id}`
  - `GET /api/worlds`
  - `POST /api/worlds`
  - `PUT /api/worlds/{id}`
  - `DELETE /api/worlds/{id}`

### User (`features/user/`)
- **Model:** `User(id, email, username?, experienceLevel?, avatarSeed?, profileCompleted, dietaryRestrictions, createdAt, updatedAt)`
- **DTOs:**
  - Requests: `CreateUserRequest(email, username?)`, `AuthRequest(email)`, `UpdateUserRequest(username, experienceLevel?, dietaryRestrictions?, profileCompleted?, avatarSeed?)`
  - Responses: `UserResponse(id, email, username?, experienceLevel?, avatarSeed?, profileCompleted, dietaryRestrictions, avatar?, createdAt, updatedAt)`, `AuthResponse(id, email, username?, avatarSeed?, profileCompleted, avatar?)`, `AvatarResponse(hairStyle, hairColor, skinColor, clothingStyle, pantsColor, shirtColor)`, `AvatarPreviewResponse(seed, avatar)`
- **Error:** `UserError` sealed class — `NotFound(email)`, `Invalid(field, reason)`, `Forbidden(userId)`, `RegistrationRequired(email)`
- **Service params:** `GetUserByIdParam(userId)`, `CreateUserParam(email, username?)`, `AuthenticateUserParam(email)`, `UpdateUserParam(userId, username, experienceLevel?, dietaryRestrictions?, profileCompleted?, avatarSeed?, requestingUserId)`, `RandomizeAvatarParam(userId, requestingUserId)`, `GetAvatarParam(userId)`
- **Actions:**
  - `GetUserByIdAction`: Fetches user by UUID
  - `CreateUserAction`: Idempotent — checks getByEmail first, returns existing if found. Generates initial avatar seed via `AvatarGenerator.seedFromName(username ?: email)`. For invite-flow users (existing stub), generates seed during username update if missing.
  - `AuthenticateUserAction`: Looks up user by email; returns `RegistrationRequired` error if username is null
  - `UpdateUserAction`: Checks userId == requestingUserId for authorization. Updates experience level, dietary restrictions (via `setDietaryRestrictions`), `profileCompleted` flag (one-way: only sets to true), and optional `avatarSeed`. Re-fetches user after update + dietary restrictions for enriched response.
  - `RandomizeAvatarAction`: Generates random seed via `AvatarGenerator.randomSeed()` and computes avatar preview. Returns `AvatarPreviewResponse(seed, avatar)` without persisting — the seed is saved later via `UpdateUserAction` when the user submits the profile form.
  - `GetAvatarAction`: Gets user, generates avatar from seed via `AvatarGenerator.generate()`
- **Mappers:** `UserMapper` (fromClient, toResponse, toAuthResponse) — avatar computed from seed at mapping time. `AvatarMapper` converts `Avatar` model to `AvatarResponse` (lowercase enum names).
- **Service:** `UserService` facade (methods: getById, create, authenticate, update, randomizeAvatar, getAvatar)
- **Routes:**
  - `GET /api/users/{userId}` — get user by ID (response includes avatar)
  - `POST /api/users` — register (idempotent, returns 201, generates avatar seed)
  - `POST /api/auth` — authenticate by email (response includes profileCompleted for frontend modal logic)
  - `PUT /api/users/{userId}` — update profile (username, experienceLevel, dietaryRestrictions, profileCompleted, avatarSeed; requires `X-User-Id` header)
  - `POST /api/users/{userId}/randomize-avatar` — generate random avatar preview without persisting (returns `AvatarPreviewResponse(seed, avatar)`; requires `X-User-Id` header)
  - `GET /api/users/{userId}/avatar` — get computed avatar for user

### Plan (`features/plan/`)
- **Model:** `Plan(id, name, visibility, ownerId, createdAt, updatedAt, isMember=true)`, `PlanMember(planId, userId, username?, email?, invitationStatus?, role, avatarSeed?, createdAt)`
- **DTOs:** `CreatePlanRequest(name)`, `UpdatePlanRequest(name, visibility?)`, `AddMemberRequest(email)`, `PlanResponse(..., isMember)`, `PlanMemberResponse(..., avatarSeed?, avatar?)`
- **Error:** `PlanError` sealed class — `NotFound`, `NotOwner`, `AlreadyMember`, `NotMember`, `Invalid`
- **Service params:** `CreatePlanParam(name, userId)`, `GetPlansParam(userId)`, `UpdatePlanParam(planId, name, visibility?, userId)`, `DeletePlanParam(planId, userId)`, `GetPlanMembersParam(planId)`, `AddPlanMemberParam(planId, email)`, `RemovePlanMemberParam(planId, userId, requestingUserId)`
- **Actions:**
  - `CreatePlanAction`: Creates private plan, auto-adds creator as member
  - `GetPlansAction`: Merges user's plans + public plans (deduped), marks non-member public plans with `isMember=false`
  - `UpdatePlanAction`: Owner-only, supports updating name and/or visibility
  - `DeletePlanAction`: Owner-only
  - `GetPlanMembersAction`: Lists plan members, enriches with username, invitation status, and avatar seed (service-layer enrichment from user lookup — no plan-client changes)
  - `AddPlanMemberAction`: Uses userClient.getOrCreate, adds member, creates invitation, sends email (with dedup)
  - `RemovePlanMemberAction`: Self-remove or owner can remove
- **Service:** `PlanService` facade (takes PlanClient + UserClient + EmailClient + InvitationClient)
- **Routes:** (all require `X-User-Id` header)
  - `POST /api/plans` — create plan (201)
  - `GET /api/plans` — list plans
  - `PUT /api/plans/{planId}` — update plan name and/or visibility (owner only)
  - `DELETE /api/plans/{planId}` — delete plan (owner only, 204)
  - `GET /api/plans/{planId}/members` — list members
  - `POST /api/plans/{planId}/members` — add member by email (201)
  - `DELETE /api/plans/{planId}/members/{memberId}` — remove member (204)

### Item (`features/item/`)
- **Model:** `Item(id, planId?, userId?, name, category, quantity, packed, createdAt, updatedAt)`
- **DTOs:** `CreateItemRequest(name, category, quantity, packed, ownerType, ownerId, planId?)`, `UpdateItemRequest(name, category, quantity, packed)`, `ItemResponse(...)`
- **Error:** `ItemError` sealed class — `NotFound(itemId)`, `Invalid(field, reason)`
- **Service params:** `CreateItemParam(name, category, quantity, packed, ownerType, ownerId, planId?, requestingUserId)`, `GetItemParam(id, requestingUserId)`, `GetItemsByOwnerParam(ownerType, ownerId, planId?, requestingUserId)`, `UpdateItemParam(id, name, category, quantity, packed, requestingUserId)`, `DeleteItemParam(id, requestingUserId)`
- **Actions:**
  - `CreateItemAction`: Validates, converts ownerType/ownerId to planId/userId, creates item
  - `GetItemAction`: Fetches item by ID
  - `GetItemsByOwnerAction`: Lists items by plan or user owner; for user items, optionally filters by planId
  - `UpdateItemAction`: Updates item fields
  - `DeleteItemAction`: Deletes item
- **Service:** `ItemService` facade (takes ItemClient)
- **Routes:** (all require `X-User-Id` header)
  - `POST /api/items` — create item (201)
  - `GET /api/items?ownerType={type}&ownerId={id}&planId={planId?}` — list items by owner (planId scopes personal items to a plan)
  - `GET /api/items/{id}` — get item by ID
  - `PUT /api/items/{id}` — update item
  - `DELETE /api/items/{id}` — delete item (204)
- **Polymorphic ownership:** Items have nullable FK columns (`plan_id`, `user_id`). Shared gear: `plan_id` only. Personal gear per plan: both `plan_id` + `user_id`. At least one must be set (DB CHECK constraint). Categories are free-form strings (no DB validation). Known categories: canoe, kitchen, camp, personal, misc, food_item.

### Itinerary (`features/itinerary/`)
- **Model:** `Itinerary(id, planId, createdAt, updatedAt)`, `ItineraryEvent(id, itineraryId, title, description?, details?, eventAt, createdAt, updatedAt)`
- **DTOs:** `AddEventRequest(title, description?, details?, eventAt)`, `UpdateEventRequest(title, description?, details?, eventAt)`, `ItineraryResponse(id, planId, events, createdAt, updatedAt)`, `ItineraryEventResponse(id, itineraryId, title, description?, details?, eventAt, createdAt, updatedAt)`
- **Error:** `ItineraryError` sealed class — `PlanNotFound(planId)`, `NotFound(planId)`, `EventNotFound(eventId)`, `Invalid(field, reason)`
- **Service params:** `GetItineraryParam(planId)`, `DeleteItineraryParam(planId)`, `AddEventParam(planId, title, description?, details?, eventAt)`, `UpdateEventParam(planId, eventId, title, description?, details?, eventAt)`, `DeleteEventParam(planId, eventId)`
- **Actions:**
  - `GetItineraryAction`: Fetches itinerary by planId with events ordered by eventAt
  - `DeleteItineraryAction`: Deletes itinerary and all events (cascade)
  - `AddEventAction`: Auto-creates itinerary if none exists, then adds event
  - `UpdateEventAction`: Updates event fields
  - `DeleteEventAction`: Deletes a single event
- **Service:** `ItineraryService` facade (takes ItineraryClient + PlanClient)
- **Routes:** (all require `X-User-Id` header)
  - `GET /api/plans/{planId}/itinerary` — get itinerary with events
  - `DELETE /api/plans/{planId}/itinerary` — delete itinerary (204)
  - `POST /api/plans/{planId}/itinerary/events` — add event (201, auto-creates itinerary)
  - `PUT /api/plans/{planId}/itinerary/events/{eventId}` — update event
  - `DELETE /api/plans/{planId}/itinerary/events/{eventId}` — delete event (204)

### Assignment (`features/assignment/`)
- **Model:** `Assignment(id, planId, name, type, maxOccupancy, ownerId, createdAt, updatedAt)`, `AssignmentMember(assignmentId, userId, username?, createdAt)`, `AssignmentDetail(id, planId, name, type, maxOccupancy, ownerId, members, createdAt, updatedAt)`
- **DTOs:** `CreateAssignmentRequest(name, type, maxOccupancy?)`, `UpdateAssignmentRequest(name?, maxOccupancy?)`, `AddAssignmentMemberRequest(userId)`, `TransferOwnershipRequest(newOwnerId)`, `AssignmentResponse(...)`, `AssignmentDetailResponse(...)`, `AssignmentMemberResponse(...)`
- **Error:** `AssignmentError` sealed class — `NotFound`, `NotOwner`, `Invalid`, `AtCapacity`, `AlreadyAssigned`, `AlreadyMember`, `CannotRemoveOwner`, `PlanNotFound`, `DuplicateName`
- **Service params:** `CreateAssignmentParam(planId, name, type, maxOccupancy?, userId)`, `GetAssignmentsParam(planId, type?)`, `GetAssignmentParam(assignmentId)`, `UpdateAssignmentParam(assignmentId, name?, maxOccupancy?, userId)`, `DeleteAssignmentParam(assignmentId, userId)`, `AddAssignmentMemberParam(assignmentId, memberUserId, userId)`, `RemoveAssignmentMemberParam(assignmentId, memberUserId, userId)`, `TransferOwnershipParam(assignmentId, newOwnerId, userId)`
- **Actions:**
  - `CreateAssignmentAction`: Creates assignment within a plan, owner auto-added as member
  - `GetAssignmentsAction`: Lists assignments for a plan, optionally filtered by type
  - `GetAssignmentAction`: Gets single assignment with member details
  - `UpdateAssignmentAction`: Owner-only update of name/maxOccupancy
  - `DeleteAssignmentAction`: Owner-only deletion
  - `AddAssignmentMemberAction`: Adds member to assignment (capacity/uniqueness checks)
  - `RemoveAssignmentMemberAction`: Self-remove or owner can remove (owner cannot be removed)
  - `TransferOwnershipAction`: Owner-only transfer of ownership to another member
- **Service:** `AssignmentService` facade (takes AssignmentClient + UserClient)
- **Routes:** (all require `X-User-Id` header)
  - `POST /api/plans/{planId}/assignments` — create assignment (201)
  - `GET /api/plans/{planId}/assignments` — list assignments (optional `?type=` filter)
  - `GET /api/plans/{planId}/assignments/{assignmentId}` — get assignment detail
  - `PUT /api/plans/{planId}/assignments/{assignmentId}` — update assignment (owner only)
  - `DELETE /api/plans/{planId}/assignments/{assignmentId}` — delete assignment (owner only, 204)
  - `POST /api/plans/{planId}/assignments/{assignmentId}/members` — add member (201)
  - `DELETE /api/plans/{planId}/assignments/{assignmentId}/members/{memberUserId}` — remove member (204)
  - `PUT /api/plans/{planId}/assignments/{assignmentId}/owner` — transfer ownership

### Webhook (`features/webhook/`)
- **DTOs:** `ResendWebhookEvent(type, createdAt, data)`, `ResendWebhookData(emailId, from?, to?, subject?)`
- **Actions:**
  - `HandleResendWebhookAction`: Maps Resend webhook events to invitation status updates (sent, delivered, bounced, delayed, complained)
- **Routes:** (no auth required)
  - `POST /api/webhooks/resend` — Resend delivery webhook (always returns 200 OK)

### Ingredient (`features/recipe/`)
- **Model:** `Ingredient(id, name, category, defaultUnit, createdAt, updatedAt)` (from ingredient-client)
- **DTOs:** `CreateIngredientRequest(name, category, defaultUnit)`, `UpdateIngredientRequest(name?, category?, defaultUnit?)`, `IngredientResponse(id, name, category, defaultUnit, createdAt, updatedAt)`
- **Error:** `RecipeError` sealed class (shared with Recipe) — `Invalid(field, reason)`, `IngredientNotFound(id)`, `DuplicateIngredientName(name)`, `NotFound(id)`, `NotCreator(id, userId)`, etc.
- **Service params:** `ListIngredientsParam(userId)`, `CreateIngredientParam(userId, name, category, defaultUnit)`, `UpdateIngredientParam(ingredientId, userId, name?, category?, defaultUnit?)`
- **Actions:**
  - `ListIngredientsAction`: delegates to `ingredientClient.getAll()`
  - `CreateIngredientAction`: validates name not blank, checks for duplicate via `findByName`, creates
  - `UpdateIngredientAction`: delegates to `ingredientClient.update`; maps `NotFoundError` → `IngredientNotFound`
- **Service:** `IngredientService` facade (takes `IngredientClient`)
- **Routes:** (all require `X-User-Id` header)
  - `GET /api/ingredients` — list all ingredients
  - `POST /api/ingredients` — create ingredient (201)
  - `PUT /api/ingredients/{id}` — update ingredient

### Recipe (`features/recipe/`)
- **Models:** `Recipe(id, name, description?, webLink?, baseServings, status, createdBy, duplicateOfId?, createdAt, updatedAt)`, `RecipeIngredient(id, recipeId, ingredientId?, originalText?, quantity, unit, status, matchedIngredientId?, suggestedIngredientName?, reviewFlags, createdAt, updatedAt)` (from recipe-client)
- **DTOs:**
  - Requests: `CreateRecipeRequest`, `ImportRecipeRequest(url)`, `UpdateRecipeRequest(name?, description?, baseServings?)`, `ResolveIngredientRequest(action, ingredientId?, newIngredient?)`, `ResolveDuplicateRequest(action)`
  - Responses: `RecipeResponse`, `RecipeDetailResponse` (with `duplicateOf?` + `ingredients`), `RecipeIngredientResponse`
- **Error:** `RecipeError` sealed class — `NotFound(id)`, `NotCreator(id, userId)`, `Invalid(field, reason)`, `DuplicateWebLink(url)`, `DuplicateIngredientName(name)`, `UnresolvedIngredients(id, count)`, `UnresolvedDuplicate(id)`, `ImportFailed(url, reason)`, `ScrapeFailed(reason)`, `IngredientNotFound(id)`, `AlreadyPublished(id)`
- **Service params:** `CreateRecipeParam`, `ImportRecipeParam(userId, url)`, `GetRecipeParam(recipeId, userId)`, `ListRecipesParam(userId)`, `UpdateRecipeParam(recipeId, userId, name?, description?, baseServings?)`, `DeleteRecipeParam(recipeId, userId)`, `ResolveIngredientParam(recipeId, recipeIngredientId, userId, action, ingredientId?, newIngredient?)`, `ResolveDuplicateParam(recipeId, userId, action)`, `PublishRecipeParam(recipeId, userId)`
- **Actions:**
  - `CreateRecipeAction`: validates name/servings, checks all ingredientIds exist, creates as `published`
  - `ImportRecipeAction`: validates URL not blank, checks not duplicate webLink, fetches HTML via `java.net.http.HttpClient`, loads all ingredients, calls `RecipeScraperClient`, detects similar recipes, creates as `draft`, adds ingredients with `pending_review`/`approved` status based on `reviewFlags`
  - `GetRecipeAction`: fetches recipe + ingredients, enriches with `IngredientResponse` map, resolves `duplicateOf`
  - `ListRecipesAction`: fetches published recipes + user's own drafts, deduplicates by ID
  - `UpdateRecipeAction`: checks creator, delegates to `recipeClient.update`
  - `DeleteRecipeAction`: checks creator, delegates to `recipeClient.delete`
  - `ResolveIngredientAction`: checks creator; handles `CONFIRM_MATCH` (use matchedIngredientId), `CREATE_NEW` (create ingredient then assign), `SELECT_EXISTING` (validate ingredient exists then assign); updates recipe_ingredient to `approved`
  - `ResolveDuplicateAction`: checks creator; `NOT_DUPLICATE` clears `duplicate_of_id`; `USE_EXISTING` deletes the recipe (returns null → 204)
  - `PublishRecipeAction`: checks creator, checks not already published, checks no unresolved duplicate, checks all ingredients approved, updates status to `published`
- **Service:** `RecipeService` facade (takes `RecipeClient`, `IngredientClient`, `RecipeScraperClient`)
- **Routes:** (all require `X-User-Id` header)
  - `POST /api/recipes` — create recipe (201)
  - `POST /api/recipes/import` — import recipe from URL (201)
  - `GET /api/recipes` — list recipes (published + own drafts)
  - `GET /api/recipes/{id}` — get recipe detail
  - `PUT /api/recipes/{id}` — update recipe (creator only)
  - `DELETE /api/recipes/{id}` — delete recipe (204, creator only)
  - `PUT /api/recipes/{id}/ingredients/{ingredientId}` — resolve pending ingredient (creator only)
  - `PUT /api/recipes/{id}/resolve-duplicate` — resolve duplicate flag (creator only; 204 if USE_EXISTING)
  - `POST /api/recipes/{id}/publish` — publish recipe (creator only)

### Meal Plan (`features/mealplan/`)
- **Models:** Uses `MealPlan`, `MealPlanDay`, `MealPlanRecipe`, `ShoppingListPurchase` from meal-plan-client; `Recipe`, `RecipeIngredient` from recipe-client; `Ingredient` from ingredient-client
- **DTOs:**
  - Requests: `CreateMealPlanRequest(name, servings, scalingMode?, isTemplate?, planId?)`, `UpdateMealPlanRequest(name?, servings?, scalingMode?)`, `AddDayRequest(dayNumber)`, `AddRecipeRequest(mealType, recipeId)`, `CopyToTripRequest(planId, servings?)`, `SaveAsTemplateRequest(name)`, `UpdatePurchaseRequest(ingredientId, unit, quantityPurchased)`
  - Responses: `MealPlanDetailResponse` (nested days → meals → recipes with scaled ingredients), `ShoppingListResponse` (computed categories → items with purchase status)
- **Error:** `MealPlanError` sealed class — `MealPlanNotFound(id)`, `DayNotFound(id)`, `RecipeNotFound(id)`, `DuplicateDayNumber(dayNumber)`, `PlanAlreadyHasMealPlan(planId)`, `NotATemplate(id)`, `IsATemplate(id)`, `Invalid(field, reason)`
- **Service params:** `CreateMealPlanParam`, `GetMealPlanDetailParam`, `GetMealPlanByPlanIdParam`, `GetTemplatesParam`, `UpdateMealPlanParam`, `DeleteMealPlanParam`, `CopyToTripParam`, `SaveAsTemplateParam`, `AddDayParam`, `RemoveDayParam`, `AddRecipeToMealParam`, `RemoveRecipeFromMealParam`, `GetShoppingListParam`, `UpdatePurchaseParam`, `ResetPurchasesParam`
- **Actions:**
  - `CreateMealPlanAction`: Creates trip or template meal plan; validates plan doesn't already have one
  - `GetMealPlanDetailAction`: Fetches meal plan with full nested detail via `MealPlanDetailBuilder`
  - `GetMealPlanByPlanIdAction`: Lookup meal plan by trip planId (returns null if none)
  - `GetTemplatesAction`: Lists template meal plans for a user
  - `UpdateMealPlanAction`: Updates name, servings, scaling mode
  - `DeleteMealPlanAction`: Deletes meal plan (cascades)
  - `CopyToTripAction`: Deep-copies template to a trip (new days, recipes; no purchases)
  - `SaveAsTemplateAction`: Deep-copies trip meal plan as a template (new days, recipes; no purchases)
  - `AddDayAction`: Adds a day to the meal plan (validates unique day number)
  - `RemoveDayAction`: Removes a day from the meal plan
  - `AddRecipeToMealAction`: Adds a recipe to a specific meal type on a day
  - `RemoveRecipeFromMealAction`: Removes a recipe from a meal
  - `GetShoppingListAction`: Computes shopping list using `ShoppingListCalculator` (scale → convert → aggregate → join purchases); purchase status derived via `PurchaseStatus.derive()`. Purchases are matched to shopping list rows by ingredient, with unit conversion via `UnitConverter` when bestFit changes the row's unit (e.g. purchase in `g` matched to a `kg` row). Orphaned purchases only show as `no_longer_needed` when the ingredient is fully removed or the purchase unit is not convertible to any current row unit; zero-quantity orphans are filtered out.
  - `UpdatePurchaseAction`: Creates/updates purchase quantity for an ingredient+unit
  - `ResetPurchasesAction`: Deletes all purchases for a meal plan
  - `MealPlanDetailBuilder`: Shared builder that assembles the nested detail response (days → meals → recipes with scaled ingredients, `isFullyPurchased` flag)
- **Service:** `MealPlanService` facade (takes MealPlanClient + RecipeClient + IngredientClient)
- **Routes:** (all require `X-User-Id` header)
  - `POST /api/meal-plans` — create meal plan (201)
  - `GET /api/meal-plans/{id}` — get meal plan detail
  - `GET /api/meal-plans?planId={planId}` — get meal plan for a trip
  - `GET /api/meal-plans/templates` — list templates
  - `PUT /api/meal-plans/{id}` — update meal plan
  - `DELETE /api/meal-plans/{id}` — delete meal plan (204)
  - `POST /api/meal-plans/{id}/copy-to-trip` — copy template to trip (201)
  - `POST /api/meal-plans/{id}/save-as-template` — save trip as template (201)
  - `POST /api/meal-plans/{id}/days` — add day (201)
  - `DELETE /api/meal-plans/{mealPlanId}/days/{dayId}` — remove day (204)
  - `POST /api/meal-plans/{mealPlanId}/days/{dayId}/recipes` — add recipe to meal (201)
  - `DELETE /api/meal-plan-recipes/{mealPlanRecipeId}` — remove recipe from meal (204, separate controller)
  - `GET /api/meal-plans/{id}/shopping-list` — get computed shopping list
  - `PATCH /api/meal-plans/{id}/shopping-list` — update purchase
  - `DELETE /api/meal-plans/{id}/shopping-list` — reset all purchases (204)
- **Key design:** Shopping list quantities are fully computed at read time (no stored quantities). The `meal-plan-calculator` lib handles scaling and unit conversion. Only purchase records are stored.

### Invite Email Flow (cross-cutting: Plan + Webhook)
- When a member is added to a plan, an invitation email is sent via the `EmailClient`
- **Dedup logic:** If invitation already has status sent/delivered/delayed/complained, skip re-sending
- If status is pending/failed/bounced, a new email is sent
- **Invitation lifecycle:** pending → sent → delivered (via webhook), or pending → sent → bounced/complained (via webhook), or pending → failed (send error)
- **Email template:** `InviteEmailTemplate` — adventure-themed HTML with inviter name, plan name, CTA button
- **Local dev:** `NoOpEmailClient` logs instead of sending real emails; no `RESEND_API_KEY` needed

## Key Patterns
- `WorldMapper.fromClient()` / `UserMapper` / `PlanMapper` / `ItemMapper` / `ItineraryMapper` / `AssignmentMapper` / `AvatarMapper` adapt client models to service models
- Service param objects for all service calls
- Action classes validate, convert params, call client
- `GlobalExceptionHandler` catches `RuntimeException::class`
- `ResultExtensions.kt` maps errors to HTTP responses
- Trust-based auth via `X-User-Id` header (no tokens/passwords)
- `PlanEventPublisher` broadcasts WebSocket events after successful controller mutations
- Controllers inject `PlanEventPublisher` and call `publishUpdate(planId, resource, action)` after `Result.Success`

## Configuration
- `WorldClientConfig` — creates client via factory function
- `UserClientConfig` — creates user client via factory function
- `PlanClientConfig` — creates plan client via factory function
- `ItemClientConfig` — creates item client via factory function
- `ItineraryClientConfig` — creates itinerary client via factory function
- `WorldServiceConfig` — wires service via `@Configuration` bean
- `UserServiceConfig` — wires UserService (takes UserClient; AvatarGenerator is a static object, no DI needed)
- `PlanServiceConfig` — wires PlanService (takes PlanClient + UserClient + EmailClient + InvitationClient)
- `ItemServiceConfig` — wires ItemService (takes ItemClient)
- `ItineraryServiceConfig` — wires ItineraryService (takes ItineraryClient + PlanClient)
- `AssignmentClientConfig` — creates assignment client via factory function
- `AssignmentServiceConfig` — wires AssignmentService (takes AssignmentClient + UserClient)
- `EmailClientConfig` — creates NoOp or Resend email client based on `RESEND_API_KEY` env var (`@ConditionalOnMissingBean`)
- `InvitationClientConfig` — creates invitation client via factory function (`@ConditionalOnMissingBean`)
- `WebhookConfig` — wires HandleResendWebhookAction
- `WebSocketConfig` — STOMP endpoint `/ws`, topic broker `/topic`, app prefix `/app`
- `IngredientClientConfig` — creates ingredient client via factory function
- `RecipeClientConfig` — creates recipe client via factory function
- `RecipeScraperClientConfig` — creates NoOp or Claude-backed scraper client based on `ANTHROPIC_API_KEY` env var
- `IngredientServiceConfig` — wires IngredientService (takes IngredientClient)
- `RecipeServiceConfig` — wires RecipeService (takes RecipeClient + IngredientClient + RecipeScraperClient)
- `MealPlanClientConfig` — creates meal plan client via factory function
- `MealPlanServiceConfig` — wires MealPlanService (takes MealPlanClient + RecipeClient + IngredientClient)

## Testing
- **Unit:** `WorldServiceTest`, `UserServiceTest` (20 tests — profile update, avatar, dietary restrictions, experience level), `PlanServiceTest`, `ItemServiceTest`, `ItineraryServiceTest`, `AssignmentServiceTest`, `RecipeServiceTest` (35 tests), `MealPlanServiceTest`, `HandleResendWebhookActionTest` use FakeClient from testFixtures
- **Acceptance:** `WorldAcceptanceTest`, `UserAcceptanceTest` (43 tests — profile CRUD, avatar endpoints, dietary restrictions, plan member avatar enrichment), `PlanAcceptanceTest`, `ItemAcceptanceTest`, `ItineraryAcceptanceTest`, `AssignmentAcceptanceTest`, `InviteEmailAcceptanceTest`, `IngredientAcceptanceTest`, `RecipeAcceptanceTest`, `MealPlanAcceptanceTest` with `@SpringBootTest(RANDOM_PORT)` + Testcontainers
- **Fixtures:** `WorldFixture`, `UserFixture`, `PlanFixture`, `ItemFixture`, `ItineraryFixture`, `AssignmentFixture`, `RecipeFixture`, `MealPlanFixture` use direct SQL for test setup
- **WebSocket:** `WebSocketIntegrationTest` verifies controllers publish STOMP messages via broker channel interceptor
- **Clean slate:** Tables truncated via `@BeforeEach`
