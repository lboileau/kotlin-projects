import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { Theme } from '@radix-ui/themes';
import '@radix-ui/themes/styles.css';
import './styles/global.css';
import { ACCENT_COLOR } from './theme';
import { queryClient } from './api/queryClient';
import { AuthProvider } from './auth/AuthProvider';
import { ToastRegion } from './components/Toast';
import { router } from './router';

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
