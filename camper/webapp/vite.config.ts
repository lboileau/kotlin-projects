import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Route areas are lazy-loaded through per-folder index.ts barrels (see
// router.tsx) so every page + its sheets share one chunk. Rollup names a
// dynamic-import chunk after its own source file by default, and since
// every barrel is literally named "index.ts", they'd otherwise all come
// out as indistinguishable "index-<hash>.js" — naming them explicitly
// here keeps production chunk names readable for debugging.
const ROUTE_AREAS = ['plans', 'shopping', 'recipes', 'ingredients', 'sign-in', 'account']

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          for (const area of ROUTE_AREAS) {
            if (id.includes(`/src/pages/${area}/`)) return `route-${area}`
          }
        },
      },
    },
  },
})
