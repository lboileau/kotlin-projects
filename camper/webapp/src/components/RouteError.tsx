import { Link, useRouteError } from 'react-router-dom';
import { Button, Heading, Text } from '@radix-ui/themes';
import './RouteError.css';

// Vite's dynamic import() throws with one of these messages (wording
// varies by browser) when the chunk it asks for is no longer on the
// server — the case right after a deploy, for a tab that's been open
// since before it. There's no error code to check, just the message.
const CHUNK_LOAD_ERROR_PATTERNS = [
  'Failed to fetch dynamically imported module',
  'Importing a module script failed',
  'error loading dynamically imported module',
];

function isChunkLoadError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error);
  return CHUNK_LOAD_ERROR_PATTERNS.some((pattern) => message.includes(pattern));
}

/**
 * Route-level error boundary, wired as `errorElement` on the top-level
 * routes in router.tsx (the authenticated shell and `/sign-in`), so it
 * catches anything a page's own render throws, including a lazy route
 * chunk that no longer exists after a deploy — otherwise a route with no
 * error boundary just white-screens. It renders as part of the normal
 * route tree under `<RouterProvider>`, which is already inside the root
 * `<Theme>` in main.tsx, so Radix tokens apply without a nested `<Theme>`.
 */
export function RouteError() {
  const error = useRouteError();
  const chunkLoadError = isChunkLoadError(error);

  return (
    <div className="route-error">
      <Heading as="h1" size="5">
        {chunkLoadError ? 'App updated' : 'Something went wrong'}
      </Heading>
      <Text as="p" color="gray" size="2">
        {chunkLoadError
          ? 'This app was updated since you opened it. Reload to get the latest version.'
          : "We hit an unexpected error. Reloading usually fixes it."}
      </Text>
      <div className="route-error__actions">
        <Button size="3" variant="solid" onClick={() => window.location.reload()}>
          Reload
        </Button>
        {!chunkLoadError && (
          <Button asChild size="3" variant="soft">
            <Link to="/plans">Go to Plans</Link>
          </Button>
        )}
      </div>
    </div>
  );
}
