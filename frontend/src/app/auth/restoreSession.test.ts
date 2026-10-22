import { ErrorResponse, ErrorTimeout, User } from "oidc-client-ts";
import { afterEach, describe, expect, it, vi } from "vitest";
import { restoreSession } from "./restoreSession";

const user = new User({
  access_token: "access-token-1",
  token_type: "Bearer",
  profile: {
    sub: "subject-1",
    iss: "http://localhost:8180/realms/crewup",
    aud: "crewup-web",
    exp: 0,
    iat: 0,
  },
});

/** A UserManager whose silent sign-in ends the way the test says, standing in for Keycloak's hidden iframe. */
function fakeUserManager(signinSilent: () => Promise<User | null>) {
  return { signinSilent: vi.fn(signinSilent) };
}

describe("restoreSession", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("M0_7_restoreSessionReturnsUserWhenSsoSessionExists", async () => {
    const userManager = fakeUserManager(() => Promise.resolve(user));

    const restored = await restoreSession(userManager);

    expect(restored).toBe(user);
    expect(userManager.signinSilent).toHaveBeenCalledOnce();
  });

  it("M0_7_restoreSessionTreatsLoginRequiredAsSignedOut", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => undefined);
    // What Keycloak answers to prompt=none when there is no SSO session: the normal signed-out case.
    const userManager = fakeUserManager(() =>
      Promise.reject(new ErrorResponse({ error: "login_required" })),
    );

    const restored = await restoreSession(userManager);

    expect(restored).toBeNull();
    expect(warn).not.toHaveBeenCalled();
  });

  it("M0_7_restoreSessionTreatsOtherErrorsAsSignedOut", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => undefined);
    // For example Keycloak is down, so the iframe never answers.
    const userManager = fakeUserManager(() =>
      Promise.reject(new ErrorTimeout("IFrame timed out without a response")),
    );

    const restored = await restoreSession(userManager);

    expect(restored).toBeNull();
    // Unexpected, so it's logged: a misconfiguration shouldn't look like "signed out" with no trace.
    expect(warn).toHaveBeenCalledOnce();
  });
});
