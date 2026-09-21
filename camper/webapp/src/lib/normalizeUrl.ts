// The same recipe page is often reachable via slightly different URLs —
// http vs https, with/without "www.", a trailing slash, a "#section"
// anchor, or tracking params like utm_source — so comparing two source
// URLs for "is this the same recipe" needs to be looser than string
// equality. Used to spot an existing recipe on a 409 CONFLICT import.

function isTrackingParam(key: string): boolean {
  return key.startsWith('utm_') || key === 'fbclid' || key === 'gclid';
}

export function normalizeUrl(raw: string): string {
  try {
    const url = new URL(raw);
    const host = url.hostname.toLowerCase().replace(/^www\./, '');
    const path = url.pathname.replace(/\/+$/, '');
    const params = new URLSearchParams(url.search);
    for (const key of [...params.keys()]) {
      if (isTrackingParam(key)) params.delete(key);
    }
    params.sort();
    const query = params.toString();
    return `${host}${path}${query ? `?${query}` : ''}`.toLowerCase();
  } catch {
    // Not a parseable absolute URL — fall back to a loose string comparison.
    return raw.trim().toLowerCase();
  }
}
