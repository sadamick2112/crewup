import type { Client } from "openapi-fetch";
import type { paths } from "./schema";

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
  // eslint-disable-next-line @typescript-eslint/no-meaningless-void-operator -- red-commit stub (TEST-2): marks the parameter used
  void getAccessToken;
  throw new Error("not implemented");
}
