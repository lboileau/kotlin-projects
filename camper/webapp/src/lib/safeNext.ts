/**
 * Validates a same-app redirect target — used for the `next` query
 * param around sign-in. `next` can come from a bookmarked or shared
 * URL, so it's untrusted: never let it become an absolute/external URL,
 * and never let it point back to /sign-in itself (redirect loop).
 *
 * Resolving with `URL` against the current origin — rather than a bare
 * `startsWith('/')` check — is what actually closes the gap: browsers
 * treat a leading `\` like `/` when resolving a URL (`/\evil.com`
 * resolves to `https://evil.com`), and `new URL(...).origin` reflects
 * that, so the origin check below rejects it.
 */
export function safeNext(raw: string | null | undefined, fallback = '/plans'): string {
  if (!raw || !raw.startsWith('/') || raw.includes('\\')) {
    return fallback;
  }

  let url: URL;
  try {
    url = new URL(raw, window.location.origin);
  } catch {
    return fallback;
  }

  if (url.origin !== window.location.origin) {
    return fallback;
  }

  const path = url.pathname + url.search + url.hash;
  if (path.startsWith('/sign-in')) {
    return fallback;
  }

  return path;
}
