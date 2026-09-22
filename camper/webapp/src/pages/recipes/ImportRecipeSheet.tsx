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
import { MAX_PHOTOS, takePhotosUpTo } from '../../lib/importPhotos';
import { preparePhoto, releasePhoto, type PreparedPhoto } from './preparePhoto';
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
  const [photos, setPhotos] = useState<PreparedPhoto[]>([]);
  const [preparing, setPreparing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [duplicate, setDuplicate] = useState<RecipeResponse | null>(null);
  const errorId = useId();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Thumbnails are object URLs; let them go when the sheet does.
  const photosRef = useRef(photos);
  photosRef.current = photos;
  useEffect(() => () => photosRef.current.forEach(releasePhoto), []);

  async function handlePick(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    // Reset so picking the same file again after removing it fires onChange.
    event.target.value = '';
    if (files.length === 0) return;

    const { accepted, dropped } = takePhotosUpTo(photos.length, files);
    if (dropped > 0) {
      toast.info(accepted.length === 0 ? `You already have ${MAX_PHOTOS} photos.` : `Up to ${MAX_PHOTOS} photos — only the first ${accepted.length} added.`);
    }
    if (accepted.length === 0) return;

    setError(null);
    setPreparing(true);
    try {
      const prepared = await Promise.all(accepted.map(preparePhoto));
      if (!mountedRef.current) {
        prepared.forEach(releasePhoto);
        return;
      }
      setPhotos((current) => {
        // Re-check the cap against the current list: a second pick could resolve
        // while this one was still decoding.
        const { accepted: kept, dropped: extra } = takePhotosUpTo(current.length, prepared);
        prepared.slice(prepared.length - extra).forEach(releasePhoto);
        return [...current, ...kept];
      });
    } catch (err) {
      if (mountedRef.current) setError(err instanceof Error ? err.message : "Couldn't read that photo.");
    } finally {
      if (mountedRef.current) setPreparing(false);
    }
  }

  function removePhoto(photo: PreparedPhoto) {
    releasePhoto(photo);
    setPhotos((current) => current.filter((p) => p.id !== photo.id));
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
      if (photos.length === 0) {
        setError('Add a photo of the recipe.');
        return;
      }
      input = { images: photos.map((p) => p.image) };
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
  const canAddMore = photos.length < MAX_PHOTOS;

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
            <Text size="2" weight="medium" id={`${errorId}-photos-label`}>
              Photos of the recipe
            </Text>
            <Text size="2" color="gray">
              Up to {MAX_PHOTOS} — the ingredient list must be readable. Several pages go in reading order.
            </Text>
            {/* The real picker: hidden, opened by the button below. `accept` gives
                the camera and the photo library on a phone; no `capture`, so the
                library is still an option for a photo taken earlier. */}
            <input
              ref={fileInputRef}
              className="import-recipe-sheet__file"
              type="file"
              accept="image/*"
              multiple
              tabIndex={-1}
              aria-hidden="true"
              disabled={busy || !canAddMore}
              onChange={handlePick}
            />
            {photos.length > 0 && (
              <ul className="import-recipe-sheet__photos" aria-labelledby={`${errorId}-photos-label`}>
                {photos.map((photo, index) => (
                  <li key={photo.id} className="import-recipe-sheet__photo">
                    <img src={photo.previewUrl} alt={`Photo ${index + 1} of ${photos.length}`} />
                    <span className="import-recipe-sheet__photo-index" aria-hidden="true">
                      {index + 1}
                    </span>
                    <button
                      type="button"
                      className="import-recipe-sheet__photo-remove"
                      aria-label={`Remove photo ${index + 1}`}
                      disabled={busy}
                      onClick={() => removePhoto(photo)}
                    >
                      <Cross2Icon />
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {canAddMore && (
              <Button
                type="button"
                size="3"
                variant="soft"
                loading={preparing}
                disabled={busy || preparing}
                onClick={() => fileInputRef.current?.click()}
              >
                {photos.length === 0 ? <CameraIcon /> : <ImageIcon />}
                {photos.length === 0 ? 'Take or choose a photo' : 'Add another photo'}
              </Button>
            )}
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
          disabled={busy || preparing || (source === 'photos' && photos.length === 0)}
        >
          Import
        </Button>
      </form>
    </Sheet>
  );
}
