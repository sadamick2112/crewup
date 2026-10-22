import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { AuthContext, type AuthContextProps } from "react-oidc-context";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiClientContext } from "@/shared/api/apiClientContext";
import { createApiClient } from "@/shared/api/client";
import { expectNoAxeViolations } from "@/test/axe";
import { server } from "@/test/server";
import { HomePage } from "./HomePage";

const profile = {
  id: "01900000-0000-7000-8000-000000000001",
  displayName: "Dev User",
  email: "dev@crewup.local",
};

function respondWithProfile(): void {
  server.use(http.get("/api/me", () => HttpResponse.json(profile)));
}

/**
 * The sign-in state HomePage reads through react-oidc-context's useAuth(), as the library's AuthContext would
 * provide it. Only the fields HomePage uses are filled in.
 */
function fakeAuth(isSignedIn: boolean) {
  return {
    isLoading: false,
    isAuthenticated: isSignedIn,
    signinRedirect: vi.fn(() => Promise.resolve()),
    signoutRedirect: vi.fn(() => Promise.resolve()),
  };
}

/** Renders HomePage with its own query cache and API client per test (TEST-8). */
function renderHomePage(auth: ReturnType<typeof fakeAuth>) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const apiClient = createApiClient(() =>
    Promise.resolve(auth.isAuthenticated ? "access-token-1" : undefined),
  );

  return render(
    <AuthContext value={auth as unknown as AuthContextProps}>
      <QueryClientProvider client={queryClient}>
        <ApiClientContext value={apiClient}>
          <HomePage />
        </ApiClientContext>
      </QueryClientProvider>
    </AuthContext>,
  );
}

describe("HomePage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("M0_7_showsGreetingWhenSignedIn", async () => {
    respondWithProfile();

    renderHomePage(fakeAuth(true));

    expect(
      await screen.findByRole("heading", { name: "Hello, Dev User" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Sign in" }),
    ).not.toBeInTheDocument();
  });

  it("M0_7_showsSignInWhenSignedOut", () => {
    // No /api/me handler: a request while signed out would fail the test (onUnhandledRequest: "error").
    renderHomePage(fakeAuth(false));

    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Sign out" }),
    ).not.toBeInTheDocument();
  });

  it("M0_7_signInButtonStartsSignIn", async () => {
    const auth = fakeAuth(false);
    renderHomePage(auth);

    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(auth.signinRedirect).toHaveBeenCalledOnce();
  });

  it("M0_7_signOutButtonSignsOut", async () => {
    respondWithProfile();
    const auth = fakeAuth(true);
    renderHomePage(auth);

    await userEvent.click(
      await screen.findByRole("button", { name: "Sign out" }),
    );

    expect(auth.signoutRedirect).toHaveBeenCalledOnce();
  });

  it("M0_25_homeSignedOutHasNoAxeViolations", async () => {
    const { container } = renderHomePage(fakeAuth(false));
    await screen.findByRole("button", { name: "Sign in" });

    await expectNoAxeViolations(container);
  });

  it("M0_25_homeSignedInHasNoAxeViolations", async () => {
    respondWithProfile();
    const { container } = renderHomePage(fakeAuth(true));
    await screen.findByRole("heading", { name: "Hello, Dev User" });

    await expectNoAxeViolations(container);
  });

  it("M0_28_failedRequestShowsReferenceId", async () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => undefined);
    let sentTraceId = "";
    server.use(
      http.get("/api/me", ({ request }) => {
        // traceparent is 00-<trace ID>-<span ID>-01; the reference is the trace ID.
        sentTraceId = request.headers.get("traceparent")?.split("-")[1] ?? "";
        return HttpResponse.json(
          { status: 500, title: "Internal Server Error", traceId: sentTraceId },
          {
            status: 500,
            headers: { "Content-Type": "application/problem+json" },
          },
        );
      }),
    );

    renderHomePage(fakeAuth(true));

    const message = await screen.findByText(/Reference: [0-9a-f]{32}/);
    expect(sentTraceId).toMatch(/^[0-9a-f]{32}$/);
    expect(message).toHaveTextContent(`Reference: ${sentTraceId}`);
    expect(consoleError).toHaveBeenCalledWith(
      expect.stringContaining(sentTraceId),
      expect.anything(),
    );
  });
});
