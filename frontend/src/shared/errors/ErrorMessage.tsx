import { useEffect } from "react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { ApiError } from "@/shared/api/client";

/**
 * Tells the user something went wrong. For a failed API call it shows the trace ID as a reference, the same ID
 * found in the backend logs, and writes it to the console (M0-28, LOG-7).
 */
export function ErrorMessage({ error }: { error: Error }) {
  // A network failure (the dev server itself is down) reaches here as a TypeError, with no request ID to show.
  const traceId = error instanceof ApiError ? error.traceId : undefined;

  useEffect(() => {
    console.error(`Request failed. Reference: ${traceId ?? "none"}`, error);
  }, [error, traceId]);

  return (
    <Alert variant="destructive">
      <AlertTitle>Something went wrong.</AlertTitle>
      {traceId !== undefined && (
        <AlertDescription>{`Reference: ${traceId}`}</AlertDescription>
      )}
    </Alert>
  );
}
