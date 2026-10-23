import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { UserManager } from "oidc-client-ts";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { AuthProvider, hasAuthParams } from "react-oidc-context";
import { BrowserRouter, Route, Routes } from "react-router";
import { AuthCallbackPage } from "@/app/auth/AuthCallbackPage";
import { oidcConfig } from "@/app/auth/oidcConfig";
import { restoreSession } from "@/app/auth/restoreSession";
import { HomePage } from "@/core/home/HomePage";
import { ApiClientContext } from "@/shared/api/apiClientContext";
import { createApiClient } from "@/shared/api/client";
import "./index.css";

const root = document.getElementById("root");
if (root === null) {
  throw new Error("index.html has no #root element");
}

const userManager = new UserManager(oidcConfig);
const apiClient = createApiClient(
  async () => (await userManager.getUser())?.access_token,
);
const queryClient = new QueryClient();

// A URL with sign-in parameters is Keycloak answering (a redirect back, or the silent sign-in iframe), and
// AuthProvider handles it. Otherwise try to restore the session before the first render, so a reload doesn't
// flash "Sign in" before the greeting (M0-7).
if (!hasAuthParams()) {
  await restoreSession(userManager);
}

createRoot(root).render(
  <StrictMode>
    <AuthProvider userManager={userManager}>
      <QueryClientProvider client={queryClient}>
        <ApiClientContext value={apiClient}>
          <BrowserRouter>
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/auth/callback" element={<AuthCallbackPage />} />
            </Routes>
          </BrowserRouter>
        </ApiClientContext>
      </QueryClientProvider>
    </AuthProvider>
  </StrictMode>,
);
