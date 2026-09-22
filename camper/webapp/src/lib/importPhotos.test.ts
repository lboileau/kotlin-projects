import { describe, expect, it } from 'vitest';
import { MAX_PHOTOS, MAX_PHOTO_EDGE, fitWithin, splitDataUrl, takePhotosUpTo } from './importPhotos';

describe('fitWithin', () => {
  it('leaves an image that already fits alone', () => {
    expect(fitWithin(800, 600)).toEqual({ width: 800, height: 600 });
    expect(fitWithin(MAX_PHOTO_EDGE, 100)).toEqual({ width: MAX_PHOTO_EDGE, height: 100 });
  });

  it('scales a landscape photo down by its width', () => {
    expect(fitWithin(4032, 3024)).toEqual({ width: 1568, height: 1176 });
  });

  it('scales a portrait photo down by its height', () => {
    expect(fitWithin(3024, 4032)).toEqual({ width: 1176, height: 1568 });
  });

  it('never scales up and never returns zero', () => {
    expect(fitWithin(10, 10, 100)).toEqual({ width: 10, height: 10 });
    expect(fitWithin(10000, 1, 100)).toEqual({ width: 100, height: 1 });
  });

  it('takes a custom edge', () => {
    expect(fitWithin(2000, 1000, 500)).toEqual({ width: 500, height: 250 });
  });
});

describe('splitDataUrl', () => {
  it('splits a base64 data url into media type and data', () => {
    expect(splitDataUrl('data:image/jpeg;base64,/9j/4AAQ')).toEqual({ mediaType: 'image/jpeg', data: '/9j/4AAQ' });
  });

  it('lowercases the media type', () => {
    expect(splitDataUrl('data:IMAGE/PNG;base64,iVBOR')).toEqual({ mediaType: 'image/png', data: 'iVBOR' });
  });

  it('rejects non-base64 and empty data urls', () => {
    expect(splitDataUrl('data:text/plain,hello')).toBeNull();
    expect(splitDataUrl('data:image/jpeg;base64,')).toBeNull();
    expect(splitDataUrl('https://example.com/a.jpg')).toBeNull();
    expect(splitDataUrl('')).toBeNull();
  });
});

describe('takePhotosUpTo', () => {
  it('accepts everything while there is room', () => {
    expect(takePhotosUpTo(0, ['a', 'b'])).toEqual({ accepted: ['a', 'b'], dropped: 0 });
  });

  it('fills the remaining slots in order and reports the rest as dropped', () => {
    expect(takePhotosUpTo(1, ['a', 'b', 'c', 'd'])).toEqual({ accepted: ['a', 'b'], dropped: 2 });
  });

  it('accepts nothing when already full', () => {
    expect(takePhotosUpTo(3, ['a'])).toEqual({ accepted: [], dropped: 1 });
    expect(takePhotosUpTo(5, ['a'])).toEqual({ accepted: [], dropped: 1 });
    expect(MAX_PHOTOS).toBe(3);
  });
});
