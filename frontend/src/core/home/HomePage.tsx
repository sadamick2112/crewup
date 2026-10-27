import { useAuth } from "react-oidc-context";
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle } from "@/components/ui/card";
import { useMe } from "@/core/me/useMe";
import { ErrorMessage } from "@/shared/errors/ErrorMessage";

/** Signed out: a sign-in button. Signed in: a greeting from `GET /api/me` and a sign-out button (M0-7). */
export function HomePage() {
  const auth = useAuth();
  const me = useMe(auth.isAuthenticated);

  // Mobile-first (TS-12): one column that fits 360 px, capped in width on larger screens (M0-11).
  return (
    <main className="mx-auto flex w-full max-w-md flex-col gap-4 p-4">
      <p className="text-lg font-semibold">CrewUp</p>
      {auth.isAuthenticated ? (
        <>
          {me.data && (
            <Card>
              <CardHeader>
                <CardTitle>
                  <h1 className="text-xl">Hello, {me.data.displayName}</h1>
                </CardTitle>
              </CardHeader>
            </Card>
          )}
          {me.error && <ErrorMessage error={me.error} />}
          <Button
            variant="outline"
            className="self-start"
            onClick={() => void auth.signoutRedirect()}
          >
            Sign out
          </Button>
        </>
      ) : (
        <>
          <h1 className="text-xl">Welcome</h1>
          <Button
            className="self-start"
            onClick={() => void auth.signinRedirect()}
          >
            Sign in
          </Button>
        </>
      )}
    </main>
  );
}
