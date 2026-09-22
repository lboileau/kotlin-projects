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
  // One photo (the server takes up to three; the sheet offers one). Picking again replaces it.
  const [photo, setPhoto] = useState<PreparedPhoto | null>(null);
  const [preparing, setPreparing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [duplicate, setDuplicate] = useState<RecipeResponse | null>(null);
  const errorId = useId();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // The thumbnail is an object URL; let it go when the sheet does.
  const photoRef = useRef(photo);
  photoRef.current = photo;
  useEffect(() => () => {
    if (photoRef.current) releasePhoto(photoRef.current);
  }, []);

  async function handlePick(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Reset so picking the same file again after removing it fires onChange.
    event.target.value = '';
    if (!file) return;

    setError(null);
    setPreparing(true);
    try {
      const prepared = await preparePhoto(file);
      if (!mountedRef.current) {
        releasePhoto(prepared);
        return;
      }
      setPhoto((current) => {
        if (current) releasePhoto(current);
        return prepared;
      });
    } catch (err) {
      if (mountedRef.current) setError(err instanceof Error ? err.message : "Couldn't read that photo.");
    } finally {
      if (mountedRef.current) setPreparing(false);
    }
  }

  function removePhoto() {
    setPhoto((current) => {
      if (current) releasePhoto(current);
      return null;
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
      if (!photo) {
        setError('Add a photo of the recipe.');
        return;
      }
      input = { images: [photo.image] };
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
              <>
                <Text size="2" weight="medium">
                  Photo of the recipe
                </Text>
                <Text size="2" color="gray">
                  The ingredient list must be readable.
                </Text>
              </>
            )}
            {/* The real picker: hidden, opened by the button below. `accept` gives
                the camera and the photo library on a phone; no `capture`, so the
                library is still an option for a photo taken earlier. */}
            <input
              ref={fileInputRef}
              className="import-recipe-sheet__file"
              type="file"
              accept="image/*"
              tabIndex={-1}
              aria-hidden="true"
              disabled={busy}
              onChange={handlePick}
            />
            {photo && (
              // While the import runs the photo shrinks to a chip: the dog and its
              // status line below need the room more than a second look at the page.
              <div className={`import-recipe-sheet__photo${busy ? ' import-recipe-sheet__photo--compact' : ''}`}>
                <img src={photo.previewUrl} alt="The recipe photo" />
                {busy ? (
                  <Text size="2" color="gray">
                    Photo attached
                  </Text>
                ) : (
                  <button
                    type="button"
                    className="import-recipe-sheet__photo-remove"
                    aria-label="Remove photo"
                    onClick={removePhoto}
                  >
                    <Cross2Icon />
                  </button>
                )}
              </div>
            )}
            {!busy && (
              <Button
                type="button"
                size="3"
                variant="soft"
                loading={preparing}
                disabled={preparing}
                onClick={() => fileInputRef.current?.click()}
              >
                {photo ? <ImageIcon /> : <CameraIcon />}
                {photo ? 'Choose a different photo' : 'Take or choose a photo'}
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
          disabled={busy || preparing || (source === 'photos' && !photo)}
        >
          Import
        </Button>
      </form>
    </Sheet>
  );
}
