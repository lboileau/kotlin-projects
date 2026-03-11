import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { SideNav } from './SideNav';
import type { ReactNode } from 'react';

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return (
    <>
      <SideNav />
      {children}
    </>
  );
}
