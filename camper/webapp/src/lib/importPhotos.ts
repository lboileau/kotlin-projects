/**
 * Pure helpers for importing a recipe from photos. The DOM half (decoding and
 * downscaling a File on a canvas) lives beside the sheet in
 * `pages/recipes/preparePhoto.ts`; everything decidable without a browser is
 * here so it can be tested.
 */

/**
 * Longest edge the photo is sized down to before upload. 1568px is the largest
 * the Claude API uses without resizing on its side, and a JPEG at that size is
 * a few hundred KB instead of the several MB a phone camera produces.
 */
export const MAX_PHOTO_EDGE = 1568;

/** What the server accepts as `mediaType`. Photos are re-encoded to JPEG, so this is only a guard. */
export const PHOTO_MEDIA_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'] as const;

/** Which part of the recipe an import photo shows. The server needs the ingredients one. */
export type ImportPhotoRole = 'ingredients' | 'instructions';

export interface ImportImage {
  mediaType: string;
  /** Raw base64, no `data:` prefix — what the API and the server both want. */
  data: string;
  role?: ImportPhotoRole;
}

/**
 * The size to draw a `width`×`height` image at so its longest edge is at most
 * `maxEdge`, keeping the aspect ratio and never scaling up. Dimensions are
 * rounded to whole pixels; a degenerate input keeps at least one pixel.
 */
export function fitWithin(width: number, height: number, maxEdge: number = MAX_PHOTO_EDGE): { width: number; height: number } {
  const longest = Math.max(width, height);
  if (longest <= maxEdge) return { width: Math.max(1, Math.round(width)), height: Math.max(1, Math.round(height)) };
  const scale = maxEdge / longest;
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  };
}

/**
 * Splits a `data:<mediaType>;base64,<data>` URL (what FileReader.readAsDataURL
 * produces) into the two parts the server wants. Returns null for anything
 * that isn't a base64 data URL.
 */
export function splitDataUrl(dataUrl: string): ImportImage | null {
  const match = /^data:([^;,]+);base64,(.*)$/s.exec(dataUrl);
  if (!match) return null;
  const [, mediaType, data] = match;
  if (!data) return null;
  return { mediaType: mediaType.toLowerCase(), data };
}
