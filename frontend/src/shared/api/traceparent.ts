/** A W3C `traceparent` header value and the trace ID inside it. */
export interface Traceparent {
  header: string;
  traceId: string;
}

/** Starts a new trace for one API request (M0-28). */
export function newTraceparent(): Traceparent {
  throw new Error("not implemented");
}
