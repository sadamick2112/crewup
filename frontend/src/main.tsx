import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";

const root = document.getElementById("root");
if (root === null) {
  throw new Error("index.html has no #root element");
}

// Placeholder until T11 adds the app shell, routing and the home page.
createRoot(root).render(
  <StrictMode>
    <main className="p-4">
      <h1 className="text-2xl font-semibold">CrewUp</h1>
    </main>
  </StrictMode>,
);
