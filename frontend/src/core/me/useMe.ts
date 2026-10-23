import { useQuery, type UseQueryResult } from "@tanstack/react-query";
import { useApiClient } from "@/shared/api/apiClientContext";
import type { components } from "@/shared/api/schema";

export type UserProfile = components["schemas"]["UserProfile"];

/** Query keys for the signed-in user's profile, defined in one place (TS-5). */
export const meKeys = {
  all: ["me"] as const,
};

/** The signed-in user's profile from `GET /api/me`. Makes no request while signed out. */
export function useMe(isSignedIn: boolean): UseQueryResult<UserProfile> {
  const client = useApiClient();

  return useQuery({
    queryKey: meKeys.all,
    queryFn: async () => {
      const { data } = await client.GET("/api/me");
      // The client throws ApiError for every error status, so a result that comes back always has data.
      return data as UserProfile;
    },
    enabled: isSignedIn,
  });
}
