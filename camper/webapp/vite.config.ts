import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Route areas are lazy-loaded through per-folder index.ts barrels (see
// router.tsx) so every page + its sheets share one chunk. Rollup names a
// dynamic-import chunk after its own source file by default, and since
// every barrel is literally named "index.ts", they'd otherwise all come
// out as indistinguishable "index-<hash>.js" — naming them explicitly
// here keeps production chunk names readable for debugging.
const ROUTE_AREAS = ['plans', 'shopping', 'recipes', 'ingredients', 'sign-in', 'account']

// Overridable so the dev server can point at a different local backend
// (e.g. a branch running on another port) without editing this file:
// `VITE_API_TARGET=http://localhost:8081 npm run dev`.
const API_TARGET = process.env.VITE_API_TARGET ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: API_TARGET,
        changeOrigin: true,
      },
      '/ws': {
        target: API_TARGET,
        ws: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          // Vendor code is split out FIRST so it never gets swept into a
          // route chunk: app code (components/, lib/, api/, queries/,
          // sync/) that many routes share is left for Rollup to hoist on
          // its own (returning undefined for it), rather than assigning it
          // to whichever route happened to be visited first in the graph.
          if (id.includes('node_modules')) {
            if (/node_modules\/(react|react-dom|scheduler)\//.test(id)) return 'vendor-react'
            if (/node_modules\/react-router/.test(id)) return 'vendor-router'
            if (/node_modules\/@tanstack/.test(id)) return 'vendor-query'
            // Radix Themes pulls in the "radix-ui" umbrella package plus
            // @floating-ui, aria-hidden and react-remove-scroll(-bar) as
            // transitive deps — keep them all together so Radix isn't
            // split across chunks with its own internals.
            if (/node_modules\/(@radix-ui|radix-ui|@floating-ui|aria-hidden|react-remove-scroll(-bar)?|use-sidecar)\//.test(id)) return 'vendor-radix'
            if (/node_modules\/@stomp/.test(id)) return 'vendor-stomp'
            return 'vendor'
          }

          for (const area of ROUTE_AREAS) {
            if (id.includes(`/src/pages/${area}/`)) return `route-${area}`
          }

          // Rollup's own hoisting of code shared between the entry and a
          // route chunk turned out NOT to happen for this app in practice:
          // lib/toastStore.ts, api/http.ts's ApiError, auth/AuthContext.tsx
          // and router.tsx itself were each inlined wholesale into whichever
          // route-* chunk their dynamic-import graph reached first, with the
          // entry then statically importing that route chunk just to reach
          // them — reintroducing the exact "swept into a route chunk" bug
          // this file exists to avoid. Pin them to one explicit shared chunk
          // instead of leaving the choice to Rollup.
          if (/\/src\/(components|lib|api|queries|sync|auth)\//.test(id)) return 'shared'
          if (/\/src\/(router|theme)\.tsx?$/.test(id)) return 'shared'
        },
      },
    },
  },
})
