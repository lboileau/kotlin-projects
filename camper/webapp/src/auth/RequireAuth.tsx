import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { Spinner } from '@radix-ui/themes';
import { useAuth } from './useAuth';
import { safeNext } from '../lib/safeNext';
import './RequireAuth.css';

/**
 * Guards every route except /sign-in. Redirects to /sign-in?next=<path>
 * when signed out, preserving the path and query string so a shared
 * link lands where it pointed after sign-in.
 */
export function RequireAuth() {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="require-auth__loading">
        <Spinner size="3" />
      </div>
    );
  }

  if (!user) {
    // Runs safeNext too (not just SignInPage's read of it) so both ends
    // of the redirect agree on what counts as a valid same-app path —
    // this path is always same-app by construction, but sharing the
    // helper keeps that invariant in one place instead of two.
    const target = safeNext(`${location.pathname}${location.search}${location.hash}`);
    return <Navigate to={`/sign-in?next=${encodeURIComponent(target)}`} replace />;
  }

  return <Outlet />;
}
