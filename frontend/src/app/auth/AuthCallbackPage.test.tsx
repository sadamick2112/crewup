import { render, screen } from "@testing-library/react";
import { AuthContext, type AuthContextProps } from "react-oidc-context";
import { MemoryRouter, Route, Routes } from "react-router";
import { describe, expect, it } from "vitest";
import { expectNoAxeViolations } from "@/test/axe";
import { AuthCallbackPage } from "./AuthCallbackPage";

/**
 * The sign-in state AuthCallbackPage reads through react-oidc-context's useAuth(), as the library's AuthContext
 * would provide it. Only the field the page uses is filled in.
 */
function fakeAuth(isLoading: boolean) {
  return { isLoading };
}

/** Renders the page at /auth/callback, with a stand-in home page so the redirect to / can be seen. */
function renderCallbackPage(auth: ReturnType<typeof fakeAuth>) {
  return render(
    <AuthContext value={auth as unknown as AuthContextProps}>
      <MemoryRouter initialEntries={["/auth/callback"]}>
        <Routes>
          <Route path="/" element={<p>Home page</p>} />
          <Route path="/auth/callback" element={<AuthCallbackPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext>,
  );
}

describe("AuthCallbackPage", () => {
  it("M0_5_callbackShowsProgressWhileSignInFinishes", () => {
    renderCallbackPage(fakeAuth(true));

    expect(screen.getByText("Signing in…")).toBeInTheDocument();
    expect(screen.queryByText("Home page")).not.toBeInTheDocument();
  });

  it("M0_5_callbackGoesHomeWhenSignInFinishes", () => {
    renderCallbackPage(fakeAuth(false));

    expect(screen.getByText("Home page")).toBeInTheDocument();
    expect(screen.queryByText("Signing in…")).not.toBeInTheDocument();
  });

  it("M0_25_callbackHasNoAxeViolations", async () => {
    const { container } = renderCallbackPage(fakeAuth(true));
    screen.getByText("Signing in…");

    await expectNoAxeViolations(container);
  });
});
