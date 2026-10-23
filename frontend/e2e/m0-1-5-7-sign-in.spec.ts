import { expect, test, type Page } from "@playwright/test";
import { expectUsableAtBothWidths } from "./checks.ts";

// DEV ONLY: the dev user from infra/keycloak/crewup-realm.json (M0-1).
const DEV_USER = { email: "dev@crewup.local", password: "dev" };
const GREETING = "Hello, Dev User";

/** Signs in through Keycloak's hosted login page and waits for the greeting (M0-5, M0-7). */
async function signIn(page: Page): Promise<void> {
  await page.goto("/");
  await page.getByRole("button", { name: "Sign in" }).click();

  await page.getByLabel("Email").fill(DEV_USER.email);
  await page.getByLabel("Password", { exact: true }).fill(DEV_USER.password);
  await page.getByRole("button", { name: "Sign In" }).click();

  await expect(page.getByRole("heading", { name: GREETING })).toBeVisible();
}

// Each test starts in a new browser context: no tokens and no Keycloak session.
test.describe("sign-in journey", () => {
  test("M0_1_M0_5_devUserSignsInAndOut", async ({ page }) => {
    await signIn(page);

    await page.getByRole("button", { name: "Sign out" }).click();

    await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();
    // The Keycloak session ended too: a reload's silent sign-in finds nothing to restore.
    await page.reload();
    await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();
  });

  test("M0_7_reloadKeepsUserSignedIn", async ({ page }) => {
    await signIn(page);

    await page.reload();

    await expect(page.getByRole("heading", { name: GREETING })).toBeVisible();
    await expect(page.getByRole("button", { name: "Sign in" })).toBeHidden();
  });

  test("M0_11_M0_25_signedOutHomeIsUsableAtDesktopAndPhoneWidths", async ({
    page,
  }) => {
    await page.goto("/");
    await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();

    await expectUsableAtBothWidths(page);
  });

  test("M0_11_M0_25_signedInHomeIsUsableAtDesktopAndPhoneWidths", async ({
    page,
  }) => {
    await signIn(page);

    await expectUsableAtBothWidths(page);
  });
});
