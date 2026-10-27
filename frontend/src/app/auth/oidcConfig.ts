import {
  InMemoryWebStorage,
  WebStorageStateStore,
  type UserManagerSettings,
} from "oidc-client-ts";

const callbackUrl = `${window.location.origin}/auth/callback`;

/**
 * Sign-in settings for Keycloak's `crewup-web` client (plan §6). Settings only: the decisions live in
 * restoreSession(), which is tested.
 */
export const oidcConfig: UserManagerSettings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: "crewup-web",
  // Authorization code flow; oidc-client-ts adds PKCE (S256) automatically, and Keycloak requires it.
  response_type: "code",
  redirect_uri: callbackUrl,
  // The hidden iframe of a silent sign-in returns to the same page; the library tells the two apart.
  silent_redirect_uri: callbackUrl,
  post_logout_redirect_uri: `${window.location.origin}/`,
  // Tokens stay in memory, never in localStorage or sessionStorage, so injected scripts can't read them later
  // (spec decision 2). A reload signs in again silently instead.
  userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
  automaticSilentRenew: true,
};
