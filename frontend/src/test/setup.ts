import "@testing-library/jest-dom/vitest";
// The rule assumes Testing Library cleans up automatically, which it can only do when the test runner provides a
// global afterEach. Vitest globals are off here, and without this cleanup renders leak into the next test.
// eslint-disable-next-line testing-library/no-manual-cleanup
import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, beforeAll } from "vitest";
import { server } from "./server";

beforeAll(() => {
  server.listen({ onUnhandledRequest: "error" });
});

afterEach(() => {
  server.resetHandlers();
  cleanup();
});

afterAll(() => {
  server.close();
});
