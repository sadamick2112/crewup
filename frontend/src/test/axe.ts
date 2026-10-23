import axe from "axe-core";
import { expect } from "vitest";

// WCAG 2.2 A and AA (M0-25).
const WCAG_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"];

/** Fails the test with one line per accessibility violation found inside `container`. */
export async function expectNoAxeViolations(container: Element): Promise<void> {
  const results = await axe.run(container, {
    runOnly: { type: "tag", values: WCAG_TAGS },
    // jsdom can't compute colors, so contrast is checked in Playwright instead (plan §8).
    rules: { "color-contrast": { enabled: false } },
  });

  const violations = results.violations.map(
    (violation) =>
      `${violation.id}: ${violation.help} (${violation.nodes.map((node) => node.target.join(" ")).join(", ")})`,
  );
  expect(violations, "axe found accessibility violations").toEqual([]);
}
