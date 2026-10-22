/** A W3C `traceparent` header value and the trace ID inside it. */
export interface Traceparent {
  header: string;
  traceId: string;
}

/**
 * Starts a new trace for one API request (M0-28). The backend continues it, so its log lines for the request carry
 * the same trace ID as the browser.
 */
export function newTraceparent(): Traceparent {
  const traceId = randomId(16);
  const parentId = randomId(8);
  // Version 00, flags 01 (sampled).
  return { header: `00-${traceId}-${parentId}-01`, traceId };
}

/** A random lowercase hex ID of `byteCount` bytes. Never all zeros, which W3C Trace Context treats as invalid. */
function randomId(byteCount: number): string {
  let id: string;
  do {
    const bytes = crypto.getRandomValues(new Uint8Array(byteCount));
    id = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join(
      "",
    );
  } while (/^0+$/.test(id));
  return id;
}
