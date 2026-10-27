import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { server } from "@/test/server";
import { ApiError, createApiClient } from "./client";

const TRACEPARENT = /^00-([0-9a-f]{32})-([0-9a-f]{16})-01$/;

const profile = {
  id: "01900000-0000-7000-8000-000000000001",
  displayName: "Dev User",
  email: "dev@crewup.local",
};

/** Answers GET /api/me with the profile and records each request, so tests can check its headers. */
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

function traceIdOf(request: Request): string {
  return TRACEPARENT.exec(request.headers.get("traceparent") ?? "")![1];
}

describe("API client", () => {
  it("M0_9_clientSendsBearerAccessToken", async () => {
    const requests = respondWithProfile();
    const client = createApiClient(() => Promise.resolve("access-token-1"));

    await client.GET("/api/me");

    expect(requests[0].headers.get("Authorization")).toBe(
      "Bearer access-token-1",
    );
  });

  it("M0_9_clientSendsNoAuthorizationWhenSignedOut", async () => {
    const requests = respondWithProfile();
    const client = createApiClient(() => Promise.resolve(undefined));

    await client.GET("/api/me");

    expect(requests[0].headers.has("Authorization")).toBe(false);
  });

  it("M0_28_apiRequestsCarryTraceparent", async () => {
    const requests = respondWithProfile();
    const client = createApiClient(() => Promise.resolve("access-token-1"));

    await client.GET("/api/me");
    await client.GET("/api/me");

    const headers = requests.map((request) =>
      request.headers.get("traceparent"),
    );
    expect(headers).toEqual([
      expect.stringMatching(TRACEPARENT),
      expect.stringMatching(TRACEPARENT),
    ]);
    expect(headers[0]).not.toBe(headers[1]);
  });

  it("M0_28_failedRequestThrowsApiErrorWithTraceId", async () => {
    const requests: Request[] = [];
    server.use(
      // Like the backend: Problem Details carrying the trace ID it continued from the request's traceparent.
      http.get("/api/me", ({ request }) => {
        requests.push(request);
        return HttpResponse.json(
          {
            title: "Internal Server Error",
            status: 500,
            traceId: traceIdOf(request),
          },
          {
            status: 500,
            headers: { "Content-Type": "application/problem+json" },
          },
        );
      }),
    );
    const client = createApiClient(() => Promise.resolve("access-token-1"));

    const error: unknown = await client
      .GET("/api/me")
      .catch((thrown: unknown) => thrown);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 500,
      traceId: traceIdOf(requests[0]),
    });
  });

  it("M0_28_failedRequestWithoutProblemDetailsStillCarriesTraceId", async () => {
    const requests: Request[] = [];
    server.use(
      // Like the Vite dev proxy when the backend is down: an error status with no body.
      http.get("/api/me", ({ request }) => {
        requests.push(request);
        return new HttpResponse(null, { status: 502 });
      }),
    );
    const client = createApiClient(() => Promise.resolve("access-token-1"));

    const error: unknown = await client
      .GET("/api/me")
      .catch((thrown: unknown) => thrown);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 502,
      traceId: traceIdOf(requests[0]),
    });
  });
});
