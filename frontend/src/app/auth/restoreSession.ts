import type { User, UserManager } from "oidc-client-ts";

/** Stub for the T11 red commit. */
export function restoreSession(
  userManager: Pick<UserManager, "signinSilent">,
): Promise<User | null> {
  // eslint-disable-next-line @typescript-eslint/no-meaningless-void-operator -- red-commit stub (TEST-2): marks the parameter used
  void userManager;
  throw new Error("not implemented");
}
