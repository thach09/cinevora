import { expect, test } from "@playwright/test";

test("public same-site auth topology: login, refresh, reload, protected access, logout", async ({
  page,
  context,
}) => {
  test.skip(
    process.env.CINEVORA_PUBLIC_AUTH !== "1",
    "Explicit public/local topology gate only",
  );
  const apiOrigin = process.env.CINEVORA_API_URL;
  const username = process.env.CINEVORA_RELEASE_USERNAME;
  const password = process.env.CINEVORA_RELEASE_PASSWORD;
  expect(apiOrigin).toBeTruthy();
  expect(username).toBeTruthy();
  expect(password).toBeTruthy();
  const base = apiOrigin!.replace(/\/$/, "");

  await page.goto("/login");
  await page.getByPlaceholder("your username").fill(username!);
  await page.getByPlaceholder("••••••••").fill(password!);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(
    page.getByRole("button", { name: /Sign out|Exit/ }),
  ).toBeVisible();

  const cookies = await context.cookies(`${base}/auth/refresh`);
  const refreshCookie = cookies.find(
    (cookie) => cookie.name === "cinevora_refresh",
  );
  expect(refreshCookie).toBeDefined();
  expect(refreshCookie?.httpOnly).toBe(true);
  expect(refreshCookie?.sameSite).toBe("Lax");
  expect(refreshCookie?.path).toBe("/api/v1/auth");
  expect(refreshCookie?.domain).toBe(new URL(base).hostname);
  if (new URL(base).protocol === "https:")
    expect(refreshCookie?.secure).toBe(true);
  const storage = await page.evaluate(() => ({
    local: { ...localStorage },
    session: { ...sessionStorage },
  }));
  expect(storage.local).toEqual({});
  expect(
    Object.keys(storage.session).every(
      (key) => key === "cinevora-active-profile",
    ),
  ).toBe(true);
  expect(JSON.stringify(storage)).not.toMatch(
    /accessToken|refreshToken|Bearer /i,
  );

  const refresh = await page.evaluate(async (api) => {
    const csrfResponse = await fetch(`${api}/auth/csrf`, {
      credentials: "include",
    });
    const proof = (await csrfResponse.json()).data;
    const response = await fetch(`${api}/auth/refresh`, {
      method: "POST",
      credentials: "include",
      headers: { [proof.headerName]: proof.token },
    });
    return { csrf: csrfResponse.status, refresh: response.status };
  }, base);
  expect(refresh).toEqual({ csrf: 200, refresh: 200 });

  await page.reload();
  await expect(
    page.getByRole("button", { name: /Sign out|Exit/ }),
  ).toBeVisible();
  const accountResponse = page.waitForResponse(
    (response) =>
      response.url().includes("/api/v1/users/me") &&
      response.request().method() === "GET",
  );
  await page.goto("/account");
  expect((await accountResponse).status()).toBe(200);
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await page.waitForURL("**/login");
  expect(
    (await context.cookies(`${base}/auth/refresh`)).some(
      (cookie) => cookie.name === "cinevora_refresh",
    ),
  ).toBe(false);
  await page.reload();
  await expect(page).toHaveURL(/\/login$/);
});
