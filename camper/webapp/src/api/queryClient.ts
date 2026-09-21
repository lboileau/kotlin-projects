import { MutationCache, QueryClient } from '@tanstack/react-query';
import { toast } from '../lib/toastStore';
import { ApiError } from './http';

// Lets a mutation opt out of the global error toast via
// `useMutation({ meta: { suppressErrorToast: true }, ... })` — for
// optimistic mutations (check-off, add/remove ad hoc item, etc.) whose
// own `onError` rollback already shows a more specific toast, so they
// don't double-toast the same failure.
declare module '@tanstack/react-query' {
  interface Register {
    mutationMeta: {
      suppressErrorToast?: boolean;
    };
  }
}

function describeError(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Something went wrong.';
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: true,
      retry: 1,
    },
    mutations: {
      retry: 0,
    },
  },
  // Cache-level onError always runs, in addition to any per-mutation
  // onError (used for optimistic rollback) — this is the one place
  // errors surface as a toast, so individual mutations never need to
  // catch-and-toast themselves, unless they opt out via
  // `meta.suppressErrorToast` above.
  mutationCache: new MutationCache({
    onError: (error, _variables, _onMutateResult, mutation) => {
      if (mutation.meta?.suppressErrorToast) return;
      toast.error(describeError(error));
    },
  }),
});
