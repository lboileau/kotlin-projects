import { useRef, useState, type ChangeEvent } from 'react';
import { Button, Text } from '@radix-ui/themes';
import { CameraIcon } from '@radix-ui/react-icons';
import { useLocation } from 'react-router-dom';
import { SheetLink } from '../../components/SheetLink';
import { useAddRecipePhoto } from '../../queries/recipes';
import { ApiError } from '../../api/http';
import { toast } from '../../lib/toastStore';
import { preparePhoto, releasePhoto } from './preparePhoto';
import type { RecipeDetailResponse } from '../../api/recipes';
import './RecipePhotos.css';

const MAX_PHOTOS_PER_RECIPE = 6;

/**
 * The Photos tab: a grid of the recipe's photos, each opening the viewer
 * sheet, plus "Add photo". Adding uploads at once through the same downscale
 * pipeline the import uses — it is a create, like the rapid ingredient add,
 * not part of the edit form — and the recipe refetches to show it.
 */
export function RecipePhotos({ recipe, mayEdit }: { recipe: RecipeDetailResponse; mayEdit: boolean }) {
  // The viewer sheet is a child route of both the recipe page and the edit
  // form, so the link is relative to whichever this grid is on.
  const location = useLocation();
  const addPhoto = useAddRecipePhoto(recipe.id);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [preparing, setPreparing] = useState(false);
  const busy = preparing || addPhoto.isPending;
  const full = recipe.photos.length >= MAX_PHOTOS_PER_RECIPE;

  async function handlePick(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setPreparing(true);
    try {
      const prepared = await preparePhoto(file);
      try {
        await addPhoto.mutateAsync(prepared.image);
        toast.info('Photo added.');
      } finally {
        releasePhoto(prepared);
      }
    } catch (err) {
      if (err instanceof ApiError) return; // the global mutation toast said what failed
      toast.error(err instanceof Error ? err.message : "Couldn't read that photo.");
    } finally {
      setPreparing(false);
    }
  }

  return (
    <div className="recipe-photos">
      {recipe.photos.length === 0 ? (
        <Text as="p" size="2" color="gray">
          No photos yet.
        </Text>
      ) : (
        <ul className="recipe-photos__grid">
          {recipe.photos.map((photo, index) => (
            <li key={photo.id}>
              <SheetLink to={`${location.pathname.replace(/\/$/, '')}/photos/${photo.id}${location.search}`} className="recipe-photos__photo">
                <img src={photo.url} alt={`Photo ${index + 1} of ${recipe.photos.length}`} loading="lazy" />
                {photo.source === 'import' && (
                  <span className="recipe-photos__badge">{photo.role ?? 'import'}</span>
                )}
              </SheetLink>
            </li>
          ))}
        </ul>
      )}
      {mayEdit && (
        <>
          <input
            ref={fileInputRef}
            className="recipe-photos__file"
            type="file"
            accept="image/*"
            tabIndex={-1}
            aria-hidden="true"
            disabled={busy || full}
            onChange={handlePick}
          />
          <Button variant="soft" size="3" loading={busy} disabled={busy || full} onClick={() => fileInputRef.current?.click()}>
            <CameraIcon /> {full ? `Up to ${MAX_PHOTOS_PER_RECIPE} photos` : 'Add photo'}
          </Button>
        </>
      )}
    </div>
  );
}

