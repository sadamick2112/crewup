import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import type { ReactNode } from "react";
import { describe, expect, it } from "vitest";
import { ApiClientContext } from "@/shared/api/apiClientContext";
import { createApiClient } from "@/shared/api/client";
import { server } from "@/test/server";
import { useMe } from "./useMe";

const profile = {
  id: "01900000-0000-7000-8000-000000000001",
  displayName: "Dev User",
  email: "dev@crewup.local",
};

/** Records each GET /api/me and answers with the profile. */
function respondWithProfile(): Request[] {
  const requests: Request[] = [];
  server.use(
    http.get("/api/me", ({ request }) => {
      requests.push(request);
      return HttpResponse.json(profile);
    }),
  );
  return requests;
}

/** A fresh query cache and API client per test, so no test sees another's data (TEST-8). */
function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const apiClient = createApiClient(() => Promise.resolve("access-token-1"));

  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <ApiClientContext value={apiClient}>{children}</ApiClientContext>
      </QueryClientProvider>
    );
  };
}

describe("useMe", () => {
  it("M0_9_useMeReturnsProfile", async () => {
    respondWithProfile();

    const { result } = renderHook(() => useMe(true), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.data).toEqual(profile);
    });
  });

  it("M0_9_useMeStaysIdleWhenSignedOut", () => {
    const requests = respondWithProfile();

    const { result } = renderHook(() => useMe(false), {
      wrapper: createWrapper(),
    });

    // An enabled query reports "fetching" from its first render, so "idle" here means no request was started.
    expect(result.current.fetchStatus).toBe("idle");
    expect(result.current.data).toBeUndefined();
    expect(requests).toEqual([]);
  });
});
