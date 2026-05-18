const { test, expect } = require('@playwright/test');

const USERNAME = 'joe';
const PASSWORD = 'pass';

async function loginWithCas(page) {
  await page.goto('/');
  await page.locator('#login').click();
  await expect(page.locator('#username')).toBeVisible();
  await page.locator('#username').fill(USERNAME);
  await page.locator('#password').fill(PASSWORD);
  await page.locator('[name="submitBtn"]').click();
}

test.describe.configure({ mode: 'serial' });

test('Login to CAS...', async ({ page }) => {
  await loginWithCas(page);
});

