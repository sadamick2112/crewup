import createClient, { type Client } from "openapi-fetch";
import type { paths } from "./schema";
import { newTraceparent } from "./traceparent";

/** The typed CrewUp API client, generated from api/openapi.json (M0-9). */
export type ApiClient = Client<paths>;

/** Returns the signed-in user's access token, or undefined when signed out. */
export type AccessTokenProvider = () => Promise<string | undefined>;

/** An API call that failed. `traceId` is the reference shown to the user and found in the backend logs (M0-28). */
export class ApiError extends Error {
  readonly status: number;
  readonly traceId: string;

  constructor(status: number, traceId: string) {
    super(`API request failed with status ${String(status)}`);
    this.name = "ApiError";
    this.status = status;
    this.traceId = traceId;
  }
}

/** Creates the client. `getAccessToken` is asked on every request, so a renewed token is used right away. */
export function createApiClient(
  getAccessToken: AccessTokenProvider,
): ApiClient {
  // A fetch wrapper rather than openapi-fetch middleware: the trace ID stays a local variable from sending the
  // request to reading its response, instead of being parsed back out of the request headers.
  async function fetchWithTraceAndToken(request: Request): Promise<Response> {
    const { header, traceId } = newTraceparent();
    request.headers.set("traceparent", header);
    const accessToken = await getAccessToken();
    if (accessToken !== undefined) {
      request.headers.set("Authorization", `Bearer ${accessToken}`);
    }

    const response = await fetch(request);
    // Use the trace ID we sent, not one from the body: an error from the dev proxy (backend down) has no body.
    if (!response.ok) {
      throw new ApiError(response.status, traceId);
    }
    return response;
  }

  return createClient<paths>({
    // The API shares the page's origin (the Vite proxy in development, plan §5). Spelled out because fetch
    // outside a browser (Node, in tests) can't resolve a relative URL.
    baseUrl: window.location.origin,
    fetch: fetchWithTraceAndToken,
  });
}
