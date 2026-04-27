import '@testing-library/jest-dom/vitest';

// @testing-library/react auto-cleanup checks for a global `afterEach` function,
// but vitest uses globals: false, so we must register cleanup manually.
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

afterEach(() => {
  cleanup();
});
