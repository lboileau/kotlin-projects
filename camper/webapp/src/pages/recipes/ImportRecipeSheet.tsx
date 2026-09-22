import { useEffect, useId, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Button, Callout, SegmentedControl, Text, TextField } from '@radix-ui/themes';
import { CameraIcon, Cross2Icon, ExclamationTriangleIcon, ImageIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { useImportRecipe, recipesKey } from '../../queries/recipes';
import { ApiError } from '../../api/http';
import { toast } from '../../lib/toastStore';
import { DogChef } from './DogChef';
import { normalizeUrl } from '../../lib/normalizeUrl';
import { preparePhoto, releasePhoto, type PreparedPhoto } from './preparePhoto';
import type { ImportPhotoRole } from '../../lib/importPhotos';
import { router } from '../../router';
import type { RecipeResponse } from '../../api/recipes';
import './ImportRecipeSheet.css';

type Source = 'url' | 'photos';

function parseSource(value: string | null): Source {
  return value === 'photos' ? 'photos' : 'url';
}

function describeImportError(err: unknown, source: Source, submittedUrl: string, cached: RecipeResponse[]): { message: string; duplicate: RecipeResponse | null } {
  if (!(err instanceof ApiError)) {
    return { message: 'Something went wrong. Try again.', duplicate: null };
  }
  if (err.code === 'CONFLICT') {
    const duplicate = cached.find((r) => r.webLink && normalizeUrl(r.webLink) === normalizeUrl(submittedUrl)) ?? null;
    return { message: err.message, duplicate };
  }
  if (err.code === 'IMPORT_FAILED') {
    return { message: "Couldn't reach that page. Check the link and try again.", duplicate: null };
  }
  if (err.code === 'SCRAPE_FAILED') {
    return {
      message: source === 'photos'
        ? "Couldn't read a recipe from those photos. Try a clearer shot that shows the ingredient list."
        : "Couldn't find a recipe on that page.",
      duplicate: null,
    };
  }
  return { message: err.message, duplicate: null };
}

export function ImportRecipeSheet() {
  const importRecipe = useImportRecipe();
  const sheet = useSheet('/recipes', {
    // Never let an accidental overlay tap or Escape silently drop a request already in flight.
    canClose: () => !importRecipe.isPending,
    onBlockedClose: () => toast.info('Still reading the recipe — hang tight.'),
  });
  const queryClient = useQueryClient();

  // The import can take up to a minute; the browser BACK button (unlike
  // overlay/Escape/X, which the sheet's `canClose` already blocks)
  // unmounts this sheet without asking, so the async work below must
  // check this before touching component state or navigating out from
  // under whatever the user is looking at by the time it resolves.
  const mountedRef = useRef(true);
  useEffect(() => {
    // Set on every mount, not just via the ref's initial value: StrictMode
    // mounts, unmounts and remounts in development, and a cleanup-only
    // effect would leave this false for the whole life of the component.
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // Which source is showing lives in the URL (every screen state has one),
  // written with `replace` so Back leaves the sheet rather than flipping tabs.
  const [searchParams, setSearchParams] = useSearchParams();
  const source = parseSource(searchParams.get('source'));
  function setSource(next: Source) {
    setSearchParams(
      (params) => {
        if (next === 'url') params.delete('source');
        else params.set('source', next);
        return params;
      },
      { replace: true },
    );
    setError(null);
    setDuplicate(null);
  }

  const [url, setUrl] = useState('');
  // Two named photos: the ingredient list (required) and the method (optional).
  // Picking a slot again replaces it. The server takes at most one per role.
  const [photos, setPhotos] = useState<Record<ImportPhotoRole, PreparedPhoto | null>>({ ingredients: null, instructions: null });
  const [preparing, setPreparing] = useState<ImportPhotoRole | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [duplicate, setDuplicate] = useState<RecipeResponse | null>(null);
  const errorId = useId();

  // Thumbnails are object URLs; let them go when the sheet does.
  const photosRef = useRef(photos);
  photosRef.current = photos;
  useEffect(() => () => {
    Object.values(photosRef.current).forEach((photo) => photo && releasePhoto(photo));
  }, []);

  async function handlePick(role: ImportPhotoRole, file: File) {
    setError(null);
    setPreparing(role);
    try {
      const prepared = await preparePhoto(file);
      if (!mountedRef.current) {
        releasePhoto(prepared);
        return;
      }
      setPhotos((current) => {
        if (current[role]) releasePhoto(current[role]!);
        return { ...current, [role]: prepared };
      });
    } catch (err) {
      if (mountedRef.current) setError(err instanceof Error ? err.message : "Couldn't read that photo.");
    } finally {
      if (mountedRef.current) setPreparing(null);
    }
  }

  function removePhoto(role: ImportPhotoRole) {
    setPhotos((current) => {
      if (current[role]) releasePhoto(current[role]!);
      return { ...current, [role]: null };
    });
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = url.trim();
    let input: { url: string } | { images: PreparedPhoto['image'][] };
    if (source === 'url') {
      if (!trimmed) {
        setError('Enter a URL.');
        return;
      }
      input = { url: trimmed };
    } else {
      if (!photos.ingredients) {
        setError('Add a photo of the ingredient list.');
        return;
      }
      input = {
        images: [
          { ...photos.ingredients.image, role: 'ingredients' },
          ...(photos.instructions ? [{ ...photos.instructions.image, role: 'instructions' as const }] : []),
        ],
      };
    }
    setError(null);
    setDuplicate(null);
    try {
      const created = await importRecipe.mutateAsync(input);
      // useImportRecipe's onSuccess already cached the detail and invalidated
      // ['recipes'] regardless of mount state — only the navigation needs guarding.
      if (mountedRef.current) {
        // `force`: importRecipe.isPending is still true in this tick, so the
        // canClose guard above would block this close and strand the user here.
        sheet.close({ to: `/recipes/${created.id}`, replace: true, force: true });
      } else {
        toast.info('Recipe imported.', {
          label: 'Open',
          onClick: () => router.navigate(`/recipes/${created.id}`),
        });
      }
    } catch (err) {
      const cached = queryClient.getQueryData<RecipeResponse[]>(recipesKey) ?? [];
      const { message, duplicate: found } = describeImportError(err, source, trimmed, cached);
      if (mountedRef.current) {
        setError(message);
        setDuplicate(found);
      } else {
        toast.error(message);
      }
    }
  }

  const busy = importRecipe.isPending;

  return (
    <Sheet {...sheet.sheetProps} title="Import recipe">
      <form className="import-recipe-sheet__form" onSubmit={handleSubmit}>
        <SegmentedControl.Root
          size="3"
          className="import-recipe-sheet__source"
          value={source}
          onValueChange={(next) => {
            if (!busy && next !== source) setSource(next as Source);
          }}
        >
          <SegmentedControl.Item value="url">Link</SegmentedControl.Item>
          <SegmentedControl.Item value="photos">Photos</SegmentedControl.Item>
        </SegmentedControl.Root>

        {source === 'url' && (
          <Text as="label" size="2" weight="medium" className="import-recipe-sheet__field">
            Recipe URL
            <TextField.Root
              type="url"
              inputMode="url"
              size="3"
              autoFocus
              autoComplete="url"
              autoCapitalize="off"
              autoCorrect="off"
              spellCheck={false}
              enterKeyHint="go"
              placeholder="https://example.com/recipe"
              value={url}
              disabled={busy}
              aria-invalid={Boolean(error)}
              aria-describedby={error ? errorId : undefined}
              onChange={(event) => setUrl(event.target.value)}
            />
          </Text>
        )}

        {source === 'photos' && (
          <div className="import-recipe-sheet__field">
            {!busy && (
              <Text size="2" color="gray">
                Two photos read best: one of the ingredient list, one of the method. Get each list fully in frame and in focus.
              </Text>
            )}
            <PhotoSlot
              role="ingredients"
              label="Ingredient list"
              help="Required. Every ingredient with its amount, top to bottom."
              photo={photos.ingredients}
              preparing={preparing === 'ingredients'}
              busy={busy}
              onPick={(file) => handlePick('ingredients', file)}
              onRemove={() => removePhoto('ingredients')}
            />
            <PhotoSlot
              role="instructions"
              label="Instructions"
              help="Optional. The method or steps. Skip it if the card has none."
              photo={photos.instructions}
              preparing={preparing === 'instructions'}
              busy={busy}
              onPick={(file) => handlePick('instructions', file)}
              onRemove={() => removePhoto('instructions')}
            />
          </div>
        )}

        {busy && (
          <div className="import-recipe-sheet__reading" role="status" aria-live="polite">
            <DogChef />
            <Text size="2" color="gray" align="center">
              Reading the recipe&hellip; this can take up to a minute.
            </Text>
          </div>
        )}

        {error && (
          <Callout.Root id={errorId} color="red" variant="surface" size="1" role="alert">
            <Callout.Icon>
              <ExclamationTriangleIcon />
            </Callout.Icon>
            <Callout.Text>
              {error}
              {duplicate && (
                <>
                  {' '}
                  <Link to={`/recipes/${duplicate.id}`}>Open existing recipe</Link>
                </>
              )}
            </Callout.Text>
          </Callout.Root>
        )}

        <Button
          type="submit"
          size="3"
          loading={busy}
          disabled={busy || preparing !== null || (source === 'photos' && !photos.ingredients)}
        >
          Import
        </Button>
      </form>
    </Sheet>
  );
}

interface PhotoSlotProps {
  role: ImportPhotoRole;
  label: string;
  help: string;
  photo: PreparedPhoto | null;
  preparing: boolean;
  busy: boolean;
  onPick: (file: File) => void;
  onRemove: () => void;
}

/**
 * One named photo: its own picker (camera *and* library on a phone — `accept`
 * without `capture`), a thumbnail with a remove button, and a chip while the
 * import runs so the dog below fits on the screen.
 */
function PhotoSlot({ role, label, help, photo, preparing, busy, onPick, onRemove }: PhotoSlotProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const labelId = useId();

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Reset so picking the same file again after removing it fires onChange.
    event.target.value = '';
    if (file) onPick(file);
  }

  if (busy && !photo) return null;

  return (
    <div className={`import-recipe-sheet__slot${busy ? ' import-recipe-sheet__slot--compact' : ''}`} role="group" aria-labelledby={labelId}>
      {!busy && (
        <div className="import-recipe-sheet__slot-head">
          <Text size="2" weight="medium" id={labelId}>
            {label}
          </Text>
          <Text size="1" color="gray">
            {help}
          </Text>
        </div>
      )}
      <input
        ref={fileInputRef}
        className="import-recipe-sheet__file"
        type="file"
        accept="image/*"
        tabIndex={-1}
        aria-hidden="true"
        disabled={busy}
        onChange={handleChange}
      />
      {photo && (
        <div className={`import-recipe-sheet__photo${busy ? ' import-recipe-sheet__photo--compact' : ''}`}>
          <img src={photo.previewUrl} alt={`${label} photo`} />
          {busy ? (
            <Text size="2" color="gray">
              {label} photo attached
            </Text>
          ) : (
            <button type="button" className="import-recipe-sheet__photo-remove" aria-label={`Remove ${label.toLowerCase()} photo`} onClick={onRemove}>
              <Cross2Icon />
            </button>
          )}
        </div>
      )}
      {!busy && (
        <Button type="button" size="3" variant="soft" loading={preparing} disabled={preparing} onClick={() => fileInputRef.current?.click()}>
          {photo ? <ImageIcon /> : <CameraIcon />}
          {photo ? 'Choose a different photo' : role === 'ingredients' ? 'Take or choose the ingredients photo' : 'Take or choose the instructions photo'}
        </Button>
      )}
    </div>
  );
}
