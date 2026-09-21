import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { getUser, type AuthUser } from '../api/auth';
import { ApiError } from '../api/http';
import { AuthContext, type AuthContextValue } from './AuthContext';
import { readStoredUser, writeStoredUser } from './storage';
import { clearNavMemory } from '../lib/navHistory';

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser());
  // Only actually loading if there's a stored user to re-validate.
  const [isLoading, setIsLoading] = useState<boolean>(() => readStoredUser() !== null);

  const signOut = useCallback(() => {
    writeStoredUser(null);
    setUser(null);
    queryClient.clear();
    clearNavMemory();
  }, [queryClient]);

  const signIn = useCallback((nextUser: AuthUser) => {
    writeStoredUser(nextUser);
    setUser(nextUser);
  }, []);

  const updateUser = useCallback((nextUser: AuthUser) => {
    writeStoredUser(nextUser);
    setUser(nextUser);
  }, []);

  // Re-validate the stored user once on load; sign out if the backend no
  // longer recognizes the id (404) or rejects it (400).
  useEffect(() => {
    const stored = readStoredUser();
    if (!stored) {
      return;
    }

    let cancelled = false;
    getUser(stored.id)
      .then((fresh) => {
        if (cancelled) return;
        writeStoredUser(fresh);
        setUser(fresh);
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        if (error instanceof ApiError && (error.status === 404 || error.status === 400)) {
          writeStoredUser(null);
          setUser(null);
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, isLoading, signIn, signOut, updateUser }),
    [user, isLoading, signIn, signOut, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
