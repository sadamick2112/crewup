import { useAuth } from "react-oidc-context";
import { Navigate } from "react-router";

/**
 * Where Keycloak sends the browser back after sign-in. AuthProvider finishes the sign-in on its own; this page
 * waits for that, then goes home. A failed sign-in also goes home, which then offers "Sign in" again.
 */
export function AuthCallbackPage() {
  const auth = useAuth();

  if (auth.isLoading) {
    return <p className="p-4">Signing in…</p>;
  }
  return <Navigate to="/" replace />;
}
