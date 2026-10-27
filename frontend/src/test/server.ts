import { setupServer } from "msw/node";

// The mock network for component tests (TEST-6). Starts with no handlers: each test adds the responses it needs
// with server.use(...), and any request without a handler fails the test.
export const server = setupServer();
