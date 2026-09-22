import { fitWithin, splitDataUrl, type ImportImage } from '../../lib/importPhotos';

/** A photo the user picked, sized down and ready to send, plus what the sheet needs to show it. */
export interface PreparedPhoto {
  /** Stable key for rendering and removal; not sent to the server. */
  id: string;
  image: ImportImage;
  /** An object URL for the thumbnail — the caller revokes it when done (`releasePhoto`). */
  previewUrl: string;
}

/** Re-encoded output. JPEG at this quality reads fine for text and keeps a page photo to a few hundred KB. */
const OUTPUT_TYPE = 'image/jpeg';
const OUTPUT_QUALITY = 0.85;

let nextId = 0;

/**
 * Decodes a picked file (any format the browser can draw, HEIC included on
 * iOS where Safari converts on pick), draws it at most `MAX_PHOTO_EDGE` on
 * its longest side with the camera's EXIF rotation applied, and re-encodes
 * it as JPEG. Rejects with a plain-English message when the file can't be
 * read as an image.
 */
export async function preparePhoto(file: File): Promise<PreparedPhoto> {
  const source = await decode(file);
  try {
    const { width, height } = fitWithin(source.width, source.height);
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) throw new Error('canvas unavailable');
    // A JPEG has no alpha: paint white first so a transparent PNG doesn't come out black.
    context.fillStyle = '#fff';
    context.fillRect(0, 0, width, height);
    context.drawImage(source.drawable, 0, 0, width, height);

    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, OUTPUT_TYPE, OUTPUT_QUALITY));
    if (!blob) throw new Error('encode failed');
    const image = splitDataUrl(await readAsDataUrl(blob));
    if (!image) throw new Error('unexpected data url');

    nextId += 1;
    return { id: `photo-${nextId}`, image, previewUrl: URL.createObjectURL(blob) };
  } finally {
    source.release();
  }
}

export function releasePhoto(photo: PreparedPhoto) {
  URL.revokeObjectURL(photo.previewUrl);
}

interface Decoded {
  drawable: CanvasImageSource;
  width: number;
  height: number;
  release: () => void;
}

async function decode(file: File): Promise<Decoded> {
  // createImageBitmap honours EXIF orientation with `from-image`, so a portrait
  // phone photo isn't sent sideways. Older Safari lacks the option (or the
  // function): fall back to an <img>, which every browser orients by default now.
  if (typeof createImageBitmap === 'function') {
    try {
      const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
      return { drawable: bitmap, width: bitmap.width, height: bitmap.height, release: () => bitmap.close() };
    } catch {
      // fall through
    }
  }
  const url = URL.createObjectURL(file);
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const element = new Image();
      element.onload = () => resolve(element);
      element.onerror = () => reject(new Error("Couldn't read that photo. Try a JPEG or PNG."));
      element.src = url;
    });
    return { drawable: img, width: img.naturalWidth, height: img.naturalHeight, release: () => URL.revokeObjectURL(url) };
  } catch (err) {
    URL.revokeObjectURL(url);
    throw err;
  }
}

function readAsDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(reader.error ?? new Error('read failed'));
    reader.readAsDataURL(blob);
  });
}
