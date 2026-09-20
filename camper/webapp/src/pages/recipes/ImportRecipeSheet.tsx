import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { Button, Callout, Spinner, Text, TextField } from '@radix-ui/themes';
import { ExclamationTriangleIcon } from '@radix-ui/react-icons';
import { Sheet } from '../../components/Sheet';
import { useSheet } from '../../components/useSheet';
import { useImportRecipe, recipesKey } from '../../queries/recipes';
import { ApiError } from '../../api/http';
import { toast } from '../../lib/toastStore';
import { normalizeUrl } from '../../lib/normalizeUrl';
import { router } from '../../router';
import type { RecipeResponse } from '../../api/recipes';
import './ImportRecipeSheet.css';

function describeImportError(err: unknown, submittedUrl: string, cached: RecipeResponse[]): { message: string; duplicate: RecipeResponse | null } {
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
    return { message: "Couldn't find a recipe on that page.", duplicate: null };
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

  const [url, setUrl] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [duplicate, setDuplicate] = useState<RecipeResponse | null>(null);
  const errorId = useId();

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = url.trim();
    if (!trimmed) {
      setError('Enter a URL.');
      return;
    }
    setError(null);
    setDuplicate(null);
    try {
      const created = await importRecipe.mutateAsync(trimmed);
      // useImportRecipe's onSuccess already cached the detail and invalidated
      // ['recipes'] regardless of mount state — only the navigation needs guarding.
      if (mountedRef.current) {
        sheet.close({ to: `/recipes/${created.id}`, replace: true });
      } else {
        toast.info('Recipe imported.', {
          label: 'Open',
          onClick: () => router.navigate(`/recipes/${created.id}`),
        });
      }
    } catch (err) {
      const cached = queryClient.getQueryData<RecipeResponse[]>(recipesKey) ?? [];
      const { message, duplicate: found } = describeImportError(err, trimmed, cached);
      if (mountedRef.current) {
        setError(message);
        setDuplicate(found);
      } else {
        toast.error(message);
      }
    }
  }

  return (
    <Sheet {...sheet.sheetProps} title="Import recipe">
      <form className="import-recipe-sheet__form" onSubmit={handleSubmit}>
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
            disabled={importRecipe.isPending}
            aria-invalid={Boolean(error)}
            aria-describedby={error ? errorId : undefined}
            onChange={(event) => setUrl(event.target.value)}
          />
        </Text>

        {importRecipe.isPending && (
          <Callout.Root color="gray" variant="surface" size="1" role="status" aria-live="polite">
            <Callout.Icon>
              <Spinner size="1" />
            </Callout.Icon>
            <Callout.Text>Reading the recipe&hellip; this can take up to a minute.</Callout.Text>
          </Callout.Root>
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

        <Button type="submit" size="3" loading={importRecipe.isPending} disabled={importRecipe.isPending}>
          Import
        </Button>
      </form>
    </Sheet>
  );
}
