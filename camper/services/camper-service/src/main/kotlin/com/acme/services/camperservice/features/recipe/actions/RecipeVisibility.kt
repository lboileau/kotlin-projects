package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.recipeclient.model.Recipe as ClientRecipe
import java.util.UUID

/**
 * A recipe is visible to a user when it is published, or when they created it
 * (their own draft). Someone else's draft is treated as not existing at all —
 * the favourite endpoints answer 404, never 403, so the existence of another
 * person's draft is never disclosed.
 *
 * Deliberately NOT applied to GetRecipeAction: `GET /api/recipes/{id}` has
 * always returned any recipe by id (RecipeDetailPage even renders a read-only
 * banner for someone else's draft). Tightening that is a behaviour change to a
 * shipped endpoint and is out of this feature's scope.
 */
internal object RecipeVisibility {
    fun isVisibleTo(recipe: ClientRecipe, userId: UUID): Boolean =
        recipe.status == "published" || recipe.createdBy == userId
}
