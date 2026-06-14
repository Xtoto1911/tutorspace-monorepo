import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { readAuthSession, getAppRouteForRole } from '../shared/auth/session';
import type { Role } from '../shared/types';

export function RequireAuth({
  allowedRoles,
  children,
}: {
  allowedRoles?: Role[];
  children: ReactNode;
}) {
  const location = useLocation();
  const session = readAuthSession();

  if (!session) {
    return <Navigate to="/auth/sign-in" replace state={{ from: location.pathname }} />;
  }

  if (allowedRoles && !allowedRoles.includes(session.user.role)) {
    return <Navigate to={getAppRouteForRole(session.user.role)} replace />;
  }

  return <>{children}</>;
}

export function AppEntryRedirect() {
  const session = readAuthSession();

  if (!session) {
    return <Navigate to="/auth/sign-in" replace />;
  }

  return <Navigate to={getAppRouteForRole(session.user.role)} replace />;
}

export function RequireGuest({ children }: { children: ReactNode }) {
  const session = readAuthSession();

  if (session) {
    return <Navigate to={getAppRouteForRole(session.user.role)} replace />;
  }

  return <>{children}</>;
}
