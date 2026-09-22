import { Outlet, useNavigate, useParams } from 'react-router-dom';
import { AlertDialog, Badge, Button, Callout, Heading, IconButton, Text } from '@radix-ui/themes';
import {
  ExclamationTriangleIcon,
  ExternalLinkIcon,
  Pencil2Icon,
  TrashIcon,
} from '@radix-ui/react-icons';
import { PageLoader } from '../../components/PageLoader';
import { PageHeader } from '../../components/PageHeader';
import { PageHero } from '../../components/PageHero';
import { HeartGlyph } from '../../components/HeartGlyph';
import { FavouriteButton } from './FavouriteButton';
import { SheetLink } from '../../components/SheetLink';
import { QueryErrorState } from '../../components/QueryErrorState';
import { BottomBar } from '../../components/BottomBar';
import { useAuth } from '../../auth/useAuth';
import {
  useDeleteRecipe,
  usePublishRecipe,
  useRecipe,
  useRecipeFavorites,
  useToggleFavorite,
} from '../../queries/recipes';
import { capitalize } from '../../lib/ingredientConstants';
import { formatFavouritedBy } from '../../lib/recipeFavorites';
import { formatQuantity } from '../../lib/formatQuantity';
import { toast } from '../../lib/toastStore';
import { ApiError } from '../../api/http';
import { RecipeReview } from './RecipeReview';
import type { RecipeIngredientResponse } from '../../api/recipes';
import './RecipeDetailPage.css';

export function RecipeDetailPage() {
  const { recipeId } = useParams<{ recipeId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data: recipe, isLoading, isError, error, refetch } = useRecipe(recipeId);
  const deleteRecipe = useDeleteRecipe();
  const publishRecipe = usePublishRecipe(recipeId ?? '');
  const toggleFavorite = useToggleFavorite();
  // The names behind the who-line, shared with the FavouritedBySheet it
  // links to. Nothing is fetched for a recipe nobody has favourited; while
  // it loads, and if it fails, `formatFavouritedBy` falls back to the count
  // alone rather than showing a half-built or empty "who".
  const favouriteCount = recipe?.favoriteCount ?? 0;
  const { data: favouritedBy } = useRecipeFavorites(recipeId, { enabled: favouriteCount > 0 });
  const favouritedBySummary = formatFavouritedBy(favouriteCount, favouritedBy, user?.id);

  const isOwner = Boolean(recipe && user && recipe.createdBy === user.id);
  const isDraft = recipe?.status === 'draft';
  // Recipes are one shared library, so anyone may edit a published recipe
  // (the backend never restricted it; only this screen did, which left most
  // recipes with no way to fix a typo or a quantity). A draft is its
  // creator's until published, and deleting stays with the creator.
  const mayEdit = !isDraft || isOwner;
  // This page is for reading. Everything about a published recipe, its
  // ingredient lines included, is edited in one place: the edit form behind
  // the pencil, which is the same form a new recipe is written in. (A
  // draft's lines are reviewed here, in RecipeReview, before publishing.)
  const canDelete = isOwner;
  const pendingCount = recipe ? recipe.ingredients.filter((line) => line.status === 'pending_review').length : 0;
  const publishBlockers: string[] = [];
  if (recipe?.duplicateOf) publishBlockers.push(`a possible duplicate of "${recipe.duplicateOf.name}" to resolve`);
  if (pendingCount > 0) publishBlockers.push(`${pendingCount} ingredient${pendingCount === 1 ? '' : 's'} to review`);
  const publishBlockedReason = publishBlockers.length > 0 ? `Before this can be published: ${publishBlockers.join(' and ')}.` : null;

  async function handleDelete() {
    if (!recipeId) return;
    await deleteRecipe.mutateAsync(recipeId);
    toast.info('Recipe deleted.');
    navigate('/recipes', { replace: true });
  }

  async function handlePublish() {
    try {
      await publishRecipe.mutateAsync();
      toast.info('Published.');
    } catch (err) {
      // The global toast already shows the server's message (already-published, still-blocked); nothing more to do here.
      if (!(err instanceof ApiError)) throw err;
    }
  }

  if (isLoading) {
    return (
      <div className="recipe-detail-page">
        <PageHeader title="Recipe" backTo="/recipes" backAcrossAreas titleInHero />
        {/* An empty box the size of the heart that replaces it once the
            recipe arrives, so the title keeps exactly the width it will
            have and never shifts sideways mid-load. */}
        <PageHero eyebrow="Recipe" title={undefined} action={<span aria-hidden="true" />} />
        <PageLoader area="recipes" label="Loading recipe" />
        <Outlet />
      </div>
    );
  }

  // A 404 means the recipe genuinely doesn't exist — retrying won't help.
  // Checked before the data-presence gate below and without requiring
  // `!recipe`, so a 404 on a background refetch (the recipe was deleted
  // while its stale detail was still on screen) still wins over showing
  // that stale data.
  const notFound = isError && error instanceof ApiError && error.status === 404;
  if (notFound) {
    return (
      <div className="recipe-detail-page">
        <PageHeader title="Recipe" backTo="/recipes" backAcrossAreas />
        <div className="recipe-detail-page__body">
          <Callout.Root color="red" variant="surface" role="alert">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>This recipe couldn&apos;t be found.</Callout.Text>
          </Callout.Root>
        </div>
        <Outlet />
      </div>
    );
  }

  // Any other failure (network, 5xx) is worth a Retry — but only when
  // there's nothing already loaded to show. This page isn't on live sync,
  // but every query still refetches on window focus, so a stale recipe
  // must survive a background refetch error rather than being blanked.
  if (isError && !recipe) {
    return (
      <div className="recipe-detail-page">
        <PageHeader title="Recipe" backTo="/recipes" backAcrossAreas />
        <div className="recipe-detail-page__body">
          <QueryErrorState message="Couldn't load this recipe." onRetry={() => void refetch()} />
        </div>
        <Outlet />
      </div>
    );
  }

  if (!recipe) {
    return null;
  }

  return (
    <div className="recipe-detail-page">
      <PageHeader
        title={recipe.name}
        backTo="/recipes"
        // Back returns to where the recipe was opened from: the Recipes list,
        // or the plan or shopping list whose link led here.
        backAcrossAreas
        titleInHero
        actions={
          mayEdit && (
            <>
              <button
                type="button"
                className="header-icon-button"
                aria-label="Edit recipe"
                onClick={() => navigate(`/recipes/${recipe.id}/edit`)}
              >
                <Pencil2Icon />
              </button>
              {/* Deleting belongs to the recipe's home in the Recipes tab, not to a plan's view of it. */}
              {canDelete && (
                <AlertDialog.Root>
                  <AlertDialog.Trigger>
                    <IconButton
                      size="3"
                      variant="soft"
                      color="red"
                      aria-label="Delete recipe"
                      className="recipe-detail-page__icon-button"
                    >
                      <TrashIcon />
                    </IconButton>
                  </AlertDialog.Trigger>
                  <AlertDialog.Content maxWidth="380px">
                    <AlertDialog.Title>Delete recipe?</AlertDialog.Title>
                    <AlertDialog.Description>
                      This removes &ldquo;{recipe.name}&rdquo; from every meal plan that uses it. This can&apos;t be undone.
                    </AlertDialog.Description>
                    <div className="recipe-detail-page__dialog-actions">
                      <AlertDialog.Cancel>
                        <Button size="3" variant="soft">
                          Cancel
                        </Button>
                      </AlertDialog.Cancel>
                      <AlertDialog.Action>
                        <Button size="3" color="red" loading={deleteRecipe.isPending} onClick={handleDelete}>
                          Delete
                        </Button>
                      </AlertDialog.Action>
                    </div>
                  </AlertDialog.Content>
                </AlertDialog.Root>
              )}
            </>
          )
        }
      />

      <PageHero
        eyebrow="Recipe"
        title={recipe.name}
        // Favouriting lives here, on the recipe itself, and nowhere else:
        // the list rows show the count but have no heart to tap.
        action={
          <FavouriteButton
            favourited={recipe.favoritedByMe}
            onToggle={(favorited) => toggleFavorite.mutate({ recipeId: recipe.id, favorited })}
          />
        }
      >
        {isDraft && (
          <Badge color="amber" variant="soft">
            Draft
          </Badge>
        )}
        <span>Serves {recipe.baseServings}</span>
        {recipe.meal && (
          <Badge variant="soft" color="gray">
            {capitalize(recipe.meal)}
          </Badge>
        )}
        {recipe.theme && (
          <Badge variant="soft" color="gray">
            {capitalize(recipe.theme)}
          </Badge>
        )}
        {/* At 0 favourites there is no line at all — the bare heart above
            is the whole feature until someone uses it. The count is a
            SheetLink because every screen state in this app has a URL. */}
        {recipe.favoriteCount > 0 && (
          <SheetLink
            to={`/recipes/${recipe.id}/favourites`}
            className="recipe-detail-page__favourite-link"
            aria-label={favouritedBySummary.label}
          >
            <HeartGlyph size={15} className="recipe-detail-page__favourite-heart" />
            <span className="recipe-detail-page__favourite-who">{favouritedBySummary.text}</span>
            <span aria-hidden="true">&rsaquo;</span>
          </SheetLink>
        )}
      </PageHero>

      <div className="recipe-detail-page__body">
        {recipe.description && (
          <Text as="p" size="2" color="gray" className="recipe-detail-page__wrap">
            {recipe.description}
          </Text>
        )}

        {recipe.webLink && (
          <a href={recipe.webLink} target="_blank" rel="noopener noreferrer" className="recipe-detail-page__source">
            Source <ExternalLinkIcon />
          </a>
        )}

        {isDraft && !isOwner && (
          <Callout.Root color="amber" variant="surface" size="1">
            <Callout.Text>This recipe is still a draft, shown here read-only.</Callout.Text>
          </Callout.Root>
        )}

        <div className="recipe-detail-page__section-header">
          <Heading size="3">Ingredients</Heading>
        </div>

        {isDraft && isOwner ? (
          <RecipeReview recipe={recipe} />
        ) : recipe.ingredients.length === 0 ? (
          <Text as="p" size="2" color="gray">
            No ingredients yet.
          </Text>
        ) : (
          <ul className="recipe-detail-page__lines">
            {recipe.ingredients.map((line) => (
              <IngredientLineRow key={line.id} line={line} recipeId={recipe.id} clickable={false} />
            ))}
          </ul>
        )}
      </div>

      <BottomBar>
        <Button
          size="3"
          variant={isDraft && isOwner ? 'soft' : 'solid'}
          className="recipe-detail-page__add-to-plan"
          asChild
        >
          <SheetLink to={`/recipes/${recipe.id}/add-to-plan`}>Add to plan</SheetLink>
        </Button>

        {isDraft && isOwner && (
          <div className="recipe-detail-page__publish">
            <Button
              size="3"
              disabled={publishBlockers.length > 0}
              loading={publishRecipe.isPending}
              onClick={handlePublish}
            >
              Publish
            </Button>
            {publishBlockedReason && (
              <Text as="p" size="1" color="gray">
                {publishBlockedReason}
              </Text>
            )}
          </div>
        )}
      </BottomBar>

      <Outlet />
    </div>
  );
}

export function IngredientLineRow({
  line,
  recipeId,
  clickable,
}: {
  line: RecipeIngredientResponse;
  recipeId: string;
  clickable: boolean;
}) {
  const name = line.ingredient?.name ?? line.matchedIngredient?.name ?? line.suggestedIngredientName ?? line.originalText ?? 'Unknown ingredient';
  const content = (
    <>
      <Text as="span" size="2" className="recipe-detail-page__line-text">
        {formatQuantity(line.quantity)} {line.unit} {name}
      </Text>
      {line.status === 'pending_review' && (
        <Badge color="amber" variant="soft" size="1">
          Needs review
        </Badge>
      )}
    </>
  );

  if (!clickable) {
    return <li className="recipe-detail-page__line">{content}</li>;
  }

  return (
    <li>
      <SheetLink to={`/recipes/${recipeId}/lines/${line.id}`} className="recipe-detail-page__line recipe-detail-page__line--link">
        {content}
      </SheetLink>
    </li>
  );
}
