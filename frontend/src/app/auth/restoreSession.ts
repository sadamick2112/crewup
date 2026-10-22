import { ErrorResponse, type User, type UserManager } from "oidc-client-ts";

/**
 * Signs the user back in after a page reload, without a visible redirect (M0-7). Tokens live only in memory, so a
 * reload loses them; a silent sign-in (a hidden iframe with `prompt=none`) gets new ones from Keycloak's SSO session.
 *
 * Returns the user, or null when they're signed out. A failure never becomes an error screen: the home page just
 * offers "Sign in" (plan §6).
 */
export async function restoreSession(
  userManager: Pick<UserManager, "signinSilent">,
): Promise<User | null> {
  try {
    return await userManager.signinSilent();
  } catch (error) {
    // login_required is Keycloak saying "no SSO session": the normal signed-out case, not worth a warning.
    const noSsoSession =
      error instanceof ErrorResponse && error.error === "login_required";
    if (!noSsoSession) {
      console.warn("Silent sign-in failed; continuing signed out", error);
    }
    return null;
  }
}
