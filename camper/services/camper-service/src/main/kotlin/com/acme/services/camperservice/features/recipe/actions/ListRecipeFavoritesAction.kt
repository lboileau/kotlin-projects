package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.recipeclient.api.GetByIdParam
import com.acme.clients.recipeclient.api.GetRecipeFavoritesParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.clients.userclient.api.GetByIdParam as UserGetByIdParam
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.recipe.dto.RecipeFavoriteUserResponse
import com.acme.services.camperservice.features.recipe.error.RecipeError
import com.acme.services.camperservice.features.recipe.mapper.RecipeMapper
import com.acme.services.camperservice.features.recipe.params.ListRecipeFavoritesParam
import com.acme.services.camperservice.features.recipe.validations.ValidateListRecipeFavorites
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Who favourited a recipe, oldest first.
 *
 * Applies the same 404-never-403 visibility rule as the two toggle actions, then
 * resolves each display name with one `userClient.getById` per row. That N+1 on
 * users is deliberate: it mirrors the established plan-member enrichment, is
 * bounded by one recipe's favouriters and is never on a list path.
 */
internal class ListRecipeFavoritesAction(
    private val recipeClient: RecipeClient,
    private val userClient: UserClient
) {
    private val logger = LoggerFactory.getLogger(ListRecipeFavoritesAction::class.java)
    private val validate = ValidateListRecipeFavorites()

    fun execute(param: ListRecipeFavoritesParam): Result<List<RecipeFavoriteUserResponse>, RecipeError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val recipe = when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> when (result.error) {
                is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
                else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
            }
        }
        if (!RecipeVisibility.isVisibleTo(recipe, param.userId)) {
            return Result.Failure(RecipeError.NotFound(param.recipeId))
        }

        logger.debug("Listing favourites of recipe={}", param.recipeId)
        val favorites = when (val result = recipeClient.getFavorites(GetRecipeFavoritesParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorites", result.error.message))
        }

        return Result.Success(
            favorites.map { RecipeMapper.toFavoriteUserResponse(it, displayName(it.userId)) }
        )
    }

    /**
     * The same rule plan members use: username, falling back to email. A failed
     * user lookup degrades to the id string rather than failing the request.
     */
    private fun displayName(userId: UUID): String =
        when (val result = userClient.getById(UserGetByIdParam(userId))) {
            is Result.Success -> result.value.username ?: result.value.email
            is Result.Failure -> userId.toString()
        }
}
