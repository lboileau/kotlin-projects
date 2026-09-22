import { useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { AlertDialog, Badge, Button, Text } from '@radix-ui/themes';
import { TrashIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { useAuth } from '../../auth/useAuth';
import { useRecipe, useRemoveRecipePhoto } from '../../queries/recipes';
import { toast } from '../../lib/toastStore';
import './RecipePhotoSheet.css';

/**
 * One photo, full width, opened from the Photos tab's grid. Removing lives
 * here (behind a confirm), not on the grid: a tap on a thumbnail should
 * only ever open it. The parent is the Photos tab, so closing lands back on
 * the grid rather than the ingredients.
 */
export function RecipePhotoSheet() {
  const { recipeId, photoId } = useParams<{ recipeId: string; photoId: string }>();
  // Mounted under both the recipe page and the edit form: the parent is
  // whichever this sheet is open over, on its Photos tab.
  const sheet = useSheet(`${useLocation().pathname.replace(/\/photos\/[^/]+\/?$/, '')}?tab=photos`);
  const { user } = useAuth();
  const { data: recipe } = useRecipe(recipeId);
  const removePhoto = useRemoveRecipePhoto(recipeId ?? '');
  const [confirming, setConfirming] = useState(false);

  const photo = recipe?.photos.find((p) => p.id === photoId);
  const index = recipe ? recipe.photos.findIndex((p) => p.id === photoId) : -1;
  const isDraft = recipe?.status === 'draft';
  const isOwner = Boolean(recipe && user && recipe.createdBy === user.id);
  const mayEdit = !isDraft || isOwner;

  async function handleRemove() {
    if (!photo) return;
    try {
      await removePhoto.mutateAsync(photo.id);
    } catch {
      return; // the global mutation toast said what failed
    }
    toast.info('Photo removed.');
    sheet.close();
  }

  const title = index >= 0 && recipe ? `Photo ${index + 1} of ${recipe.photos.length}` : 'Photo';

  return (
    <Sheet {...sheet.sheetProps} title={title} fullHeight>
      {!photo ? (
        <Text size="2" color="gray">
          This photo is no longer on the recipe.
        </Text>
      ) : (
        <div className="recipe-photo-sheet">
          <img
            className="recipe-photo-sheet__image"
            src={photo.url}
            alt={photo.role === 'ingredients' ? 'Ingredient list photo' : photo.role === 'instructions' ? 'Instructions photo' : 'Recipe photo'}
            width={photo.width ?? undefined}
            height={photo.height ?? undefined}
          />
          <div className="recipe-photo-sheet__meta">
            {photo.source === 'import' && (
              <Badge variant="soft" color="gray">
                From import{photo.role ? ` · ${photo.role}` : ''}
              </Badge>
            )}
            {mayEdit && (
              <AlertDialog.Root open={confirming} onOpenChange={setConfirming}>
                <AlertDialog.Trigger>
                  <Button variant="soft" color="red" size="3" className="recipe-photo-sheet__remove">
                    <TrashIcon /> Remove photo
                  </Button>
                </AlertDialog.Trigger>
                <AlertDialog.Content maxWidth="360px">
                  <AlertDialog.Title>Remove this photo?</AlertDialog.Title>
                  <AlertDialog.Description size="2">It will be deleted from the recipe. This can't be undone.</AlertDialog.Description>
                  <div className="recipe-photo-sheet__confirm-actions">
                    <AlertDialog.Cancel>
                      <Button variant="soft" color="gray">
                        Keep
                      </Button>
                    </AlertDialog.Cancel>
                    <AlertDialog.Action>
                      <Button color="red" loading={removePhoto.isPending} onClick={handleRemove}>
                        Remove
                      </Button>
                    </AlertDialog.Action>
                  </div>
                </AlertDialog.Content>
              </AlertDialog.Root>
            )}
          </div>
        </div>
      )}
    </Sheet>
  );
}
