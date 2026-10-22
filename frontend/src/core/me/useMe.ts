import type { UseQueryResult } from "@tanstack/react-query";
import type { components } from "@/shared/api/schema";

export type UserProfile = components["schemas"]["UserProfile"];

/** Query keys for the signed-in user's profile, defined in one place (TS-5). */
export const meKeys = {
  all: ["me"] as const,
};

/** The signed-in user's profile from `GET /api/me`. Makes no request while signed out. */
export function useMe(isSignedIn: boolean): UseQueryResult<UserProfile> {
  // eslint-disable-next-line @typescript-eslint/no-meaningless-void-operator -- red-commit stub (TEST-2): marks the parameter used
  void isSignedIn;
  throw new Error("not implemented");
}
