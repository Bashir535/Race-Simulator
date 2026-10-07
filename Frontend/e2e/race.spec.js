import { test, expect } from "@playwright/test";

async function chooseCars(page) {
  await page.goto("/");
  await page.getByRole("button", { name: /^GARAGE/ }).click();
  await expect(page.getByRole("button", { name: "Lane A", exact: true })).toHaveCount(8);
  await page.getByText("2021 BMW M3", { exact: true }).locator("..").getByRole("button", { name: "Lane A", exact: true }).click();
  await page.getByRole("button", { name: /^GARAGE/ }).click();
  await page.getByText("2022 BMW M8", { exact: true }).locator("..").getByRole("button", { name: "Lane B", exact: true }).click();
  await expect(page.getByRole("button", { name: "Compare cars", exact: true })).toBeEnabled();
}
const completed = page => page.getByText("Race complete. Results are below.", { exact: true });

test("dedicated comparison and stock races", async ({ page }) => {
  const errors = [];
  page.on("pageerror", e => errors.push(e.message));
  await page.emulateMedia({ reducedMotion: "reduce" });
  await chooseCars(page);
  await page.getByRole("button", { name: "Compare cars", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Car Specs" })).toBeVisible();
  await expect(page.getByRole("table")).toContainText("Original MSRP");
  await expect(page.getByRole("table")).toContainText("BMW");
  await expect(page.getByRole("button", { name: "Run race", exact: true })).toHaveCount(0);
  await expect(page.getByText(/Add mods|Database garage|Browser bookmarks/)).toHaveCount(0);
  await page.screenshot({ path: "test-results/compare-desktop.png", fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({ path: "test-results/compare-mobile.png" });
  await page.getByRole("button", { name: "Race these cars" }).click();
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.getByLabel("Starting speed", { exact: true }).selectOption("40");
  await page.getByLabel("Race distance", { exact: true }).selectOption("201.168");
  const request = page.waitForRequest(r => r.url().endsWith("/races/simulate"));
  await page.getByRole("button", { name: "Run race", exact: true }).click();
  const body = (await request).postDataJSON();
  expect(body.modificationsA).toBeUndefined();
  expect(body.modificationsB).toBeUndefined();
  await expect(completed(page)).toBeVisible({ timeout: 15000 });
  await page.getByRole("button", { name: "Run race", exact: true }).click();
  await expect(completed(page)).toBeVisible({ timeout: 15000 });
  await page.getByRole("button", { name: "Save to garage", exact: true }).first().click();
  await page.getByRole("button", { name: /^GARAGE/ }).click();
  await expect(page.getByLabel("Saved vehicle name")).toHaveCount(1);
  await page.reload();
  await page.getByRole("button", { name: /^GARAGE/ }).click();
  await expect(page.getByLabel("Saved vehicle name")).toHaveCount(1);
  expect(errors).toEqual([]);
});

test("live playback pauses, resumes, replays and resets", async ({ page }) => {
  await chooseCars(page);
  await page.getByLabel("Starting speed", { exact: true }).selectOption("70");
  await page.getByLabel("Race distance", { exact: true }).selectOption("201.168");
  await page.getByRole("button", { name: "Run race", exact: true }).click();
  await expect(page.getByRole("button", { name: "Pause", exact: true })).toBeEnabled({ timeout: 15000 });
  await page.getByRole("button", { name: "Pause", exact: true }).click();
  await page.getByRole("button", { name: "Resume", exact: true }).click();
  await expect(completed(page)).toBeVisible({ timeout: 15000 });
  await page.getByRole("button", { name: /Replay/ }).click();
  await expect(page.getByRole("button", { name: "Pause", exact: true })).toBeEnabled();
  await expect(completed(page)).toBeVisible({ timeout: 15000 });
  await page.getByRole("button", { name: "Clear the race and start over", exact: true }).click();
  await expect(completed(page)).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Run race", exact: true })).toBeEnabled();
});

test("backend failure can be retried", async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await chooseCars(page);
  await page.route("**/races/simulate", route => route.fulfill({ status: 503, contentType: "application/json", body: JSON.stringify({ message: "Test backend unavailable", retryable: true }) }));
  await page.getByRole("button", { name: "Run race", exact: true }).click();
  await expect(page.getByText("Test backend unavailable", { exact: false })).toBeVisible({ timeout: 15000 });
  await page.unroute("**/races/simulate");
  await page.getByRole("button", { name: "Run race", exact: true }).click();
  await expect(completed(page)).toBeVisible({ timeout: 15000 });
});
