import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { Theme } from '@radix-ui/themes';
// Modular Radix Themes CSS instead of the monolithic styles.css (which
// bundles all ~24 accent color scales): base tokens + component +
// layout + utility rules, plus only the color scales this app actually
// references — the three accent choices being compared (see theme.ts),
// the fixed gray scale, and every literal `color="…"` override used
// anywhere in the app (red/green/amber for destructive, success and
// draft states). Adding a new named color anywhere in the app needs a
// matching import here, or that color's tokens (e.g. --red-9) resolve
// to nothing.
import '@radix-ui/themes/tokens/base.css';
import '@radix-ui/themes/tokens/colors/violet.css';
import '@radix-ui/themes/tokens/colors/purple.css';
import '@radix-ui/themes/tokens/colors/iris.css';
import '@radix-ui/themes/tokens/colors/mauve.css';
import '@radix-ui/themes/tokens/colors/red.css';
import '@radix-ui/themes/tokens/colors/green.css';
import '@radix-ui/themes/tokens/colors/amber.css';
import '@radix-ui/themes/components.css';
import '@radix-ui/themes/layout.css';
import '@radix-ui/themes/utilities.css';
import './styles/global.css';
import { ACCENT_COLOR } from './theme';
import { queryClient } from './api/queryClient';
import { AuthProvider } from './auth/AuthProvider';
import { ToastRegion } from './components/Toast';
import { router } from './router';
import { installViewportGuard } from './lib/viewportGuard';
import { installViewportDebug } from './lib/viewportDebug';

installViewportGuard();
installViewportDebug();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Theme appearance="light" accentColor={ACCENT_COLOR} grayColor="mauve" radius="large" scaling="100%">
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <RouterProvider router={router} />
          <ToastRegion />
        </AuthProvider>
      </QueryClientProvider>
    </Theme>
  </StrictMode>,
);
