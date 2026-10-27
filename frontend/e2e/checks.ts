import AxeBuilder from "@axe-core/playwright";
import { expect, type Page } from "@playwright/test";

// WCAG 2.2 A and AA, the same tags as the component tests' src/test/axe.ts (M0-25).
const WCAG_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"];

/** Phone width from M0-11. */
export const MOBILE_VIEWPORT = { width: 360, height: 740 };
export const DESKTOP_VIEWPORT = { width: 1280, height: 720 };

/**
 * Runs axe on the whole page, including color contrast (which jsdom can't check), and fails with one line per
 * violation.
 */
export async function expectNoAxeViolations(page: Page): Promise<void> {
  const results = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze();

  const violations = results.violations.map(
    (violation) =>
      `${violation.id}: ${violation.help} (${violation.nodes.map((node) => node.target.join(" ")).join(", ")})`,
  );
  expect(violations, "axe found accessibility violations").toEqual([]);
}

/** The page fits the viewport's width: nothing sticks out to the side (M0-11). */
export async function expectNoHorizontalScroll(page: Page): Promise<void> {
  const overflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth -
      document.documentElement.clientWidth,
  );
  expect(overflow, "page is wider than the viewport").toBeLessThanOrEqual(0);
}

/** Runs the layout and accessibility checks at desktop and phone widths, then returns to desktop (M0-11, M0-25). */
export async function expectUsableAtBothWidths(page: Page): Promise<void> {
  for (const viewport of [DESKTOP_VIEWPORT, MOBILE_VIEWPORT]) {
    await page.setViewportSize(viewport);
    await expectNoHorizontalScroll(page);
    await expectNoAxeViolations(page);
  }
  await page.setViewportSize(DESKTOP_VIEWPORT);
}
