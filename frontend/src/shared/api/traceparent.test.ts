import { afterEach, describe, expect, it, vi } from "vitest";
import { newTraceparent } from "./traceparent";

// W3C Trace Context: version 00, 32-hex trace ID, 16-hex parent (span) ID, flags 01 (sampled).
const TRACEPARENT = /^00-([0-9a-f]{32})-([0-9a-f]{16})-01$/;

describe("newTraceparent", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("M0_28_traceparentHasValidFormatAndIsUnique", () => {
    const traceparents = Array.from({ length: 1000 }, () => newTraceparent());

    for (const { header, traceId } of traceparents) {
      const match = TRACEPARENT.exec(header);
      expect(match, header).not.toBeNull();
      expect(traceId).toBe(match![1]);
    }
    expect(new Set(traceparents.map(({ traceId }) => traceId)).size).toBe(1000);
  });

  it("M0_28_traceparentIdsAreNeverAllZeros", () => {
    // W3C Trace Context treats all-zero IDs as invalid, so a zero draw from the random source must not be used.
    vi.spyOn(crypto, "getRandomValues").mockImplementationOnce(
      (array) => array,
    );

    const { header } = newTraceparent();

    const [, traceId, parentId] = TRACEPARENT.exec(header)!;
    expect(traceId).not.toBe("0".repeat(32));
    expect(parentId).not.toBe("0".repeat(16));
  });
});
