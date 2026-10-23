import { createContext, useContext } from "react";
import type { ApiClient } from "./client";

/** Provides the app's one API client to hooks such as `useMe()`. */
export const ApiClientContext = createContext<ApiClient | null>(null);

export function useApiClient(): ApiClient {
  const client = useContext(ApiClientContext);
  if (client === null) {
    throw new Error(
      "useApiClient() needs an <ApiClientContext> above it in the component tree",
    );
  }
  return client;
}
