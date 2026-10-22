import { createContext } from "react";
import type { ApiClient } from "./client";

/** Provides the app's one API client to hooks such as `useMe()`. */
export const ApiClientContext = createContext<ApiClient | null>(null);

export function useApiClient(): ApiClient {
  throw new Error("not implemented");
}
