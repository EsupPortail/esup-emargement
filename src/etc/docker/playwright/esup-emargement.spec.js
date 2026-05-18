const { test, expect } = require('@playwright/test');

const PASSWORD = 'pass';
const ADMIN_USERNAME = 'admin';
const USERNAME = 'joe';
const CONTEXT_KEY = 'testPlaywright';
const CONTEXT_TITLE = 'testPlaywright';
const SITE_NAME = 'sitePlaywright';
const LOCATION_NAME = 'lieuPlaywright';
const SESSION_NAME = 'sessionPalywrigjht';
const JACK = 'jack@example.org';
const WILLIAM = 'william@example.org';
const SESSION_DATE = '2030-01-01';

async function loginWithCas(page, username) {
  await page.goto('/login');
  await expect(page.locator('#username')).toBeVisible();
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(PASSWORD);
  await page.locator('[name="submitBtn"]').click();
}

async function logout(page) {
  await page.getByRole('link', { name: 'Déconnexion' }).click();
  await page.goto('/login');
  await expect(page.locator('#username')).toBeVisible();
}

async function createContext(page) {
  await page.goto('/all/superadmin/context?form');
  await page.locator('input[name="key"]').fill(CONTEXT_KEY);
  await page.locator('input[name="title"]').fill(CONTEXT_TITLE);
  await page.locator('textarea[name="commentaire"]').fill('Contexte Playwright');
  await page.locator('input[type="checkbox"][name="isActif"]').check();
  await page.locator('input[type="submit"][value="Valider"]').click();
  await expect(page.locator('table tbody tr', { hasText: CONTEXT_KEY }).first().locator('td').first()).toHaveText(CONTEXT_KEY);
}

async function createContextAdmin(page) {
  await page.goto('/all/superadmin/admins?form');
  await page.locator('input[name="eppn"]').fill('joe@example.org');
  await page.locator('select[name="userRole"]').selectOption('ADMIN');
  await page.locator('select[name="context.key"]').selectOption(CONTEXT_KEY);
  await page.locator('input[type="submit"][value="Valider"]').click();
  await expect(page.locator('table tbody tr', { hasText: 'joe@example.org' }).first().locator('td').first()).toHaveText('joe@example.org');
}

async function unactivateAde(page) {
  await page.goto('/testPlaywright/admin/appliConfig');
  // désactivation d'ADE
  await page.locator('table tbody tr').filter({ hasText: 'ADE_ENABLED' }).first().locator('td.center a').nth(1).click();
  await page.locator('#boolFalse').check();
  await page.locator('input[type="submit"][value="Valider"]').click();
}

async function createCampus(page) {
  await page.goto(`/${CONTEXT_KEY}/admin/campus?form`);
  await page.locator('input[name="site"]').fill(SITE_NAME);
  await page.locator('textarea[name="description"]').fill('Site Playwright');
  await page.locator('input[type="submit"][value="Valider"]').click();
  await expect(page.locator('table tbody tr', { hasText: SITE_NAME }).first().locator('td').first()).toHaveText(SITE_NAME);
}

async function createLocation(page) {
  await page.goto(`/${CONTEXT_KEY}/admin/location?form`);
  await page.locator('input[name="nom"]').fill(LOCATION_NAME);
  await page.locator('textarea[name="adresse"]').fill('Adresse Playwright');
  await page.locator('input[name="capacite"]').fill('10');
  await page.locator('select[name="campus"]').selectOption({ label: SITE_NAME });
  await page.locator('input[type="submit"][value="Valider"]').click();
  await expect(page.locator('table tbody tr', { hasText: LOCATION_NAME }).first().locator('td').first()).toHaveText(LOCATION_NAME);
}

async function createSupervisorUser(page, eppn) {
  await page.goto(`/${CONTEXT_KEY}/admin/userApp?form`);
  await page.locator('#eppn').evaluate((el, value) => {
    el.value = value;
  }, eppn);
  await page.locator('input[name="speciality"]').fill('Playwright');
  await page.locator('select[name="userRole"]').selectOption('SUPERVISOR');
  await page.locator('input[type="submit"][value="Valider"]').click();
  await expect(page.locator('#userAppPage table tbody tr', { hasText: eppn }).first().locator('td').first()).toHaveText(eppn);
}

async function createSession(page) {
  await page.goto(`/${CONTEXT_KEY}/manager/sessionEpreuve?form`);
  await page.locator('select[name="typeSession"]').selectOption({ index: 1 });
  await page.locator('select[name="campus"]').selectOption({ label: SITE_NAME });
  await page.locator('input[name="nomSessionEpreuve"]').fill(SESSION_NAME);
  await page.locator('input[name="strDateExamen"]').fill(SESSION_DATE);
  await page.locator('input[name="heureEpreuve"]').fill('09:00');
  await page.locator('input[name="finEpreuve"]').fill('11:00');
  await page.locator('input[type="submit"][value="Valider"]').click();
  await expect(page.locator('#tableSessionEpreuve tbody tr', { hasText: SESSION_NAME }).first().locator('td').nth(2)).toHaveText(SESSION_NAME);
}

async function addTagCheck(page, sessionId, eppn) {
  await page.goto(`/${CONTEXT_KEY}/manager/tagCheck?form&sessionEpreuve=${sessionId}`);
  await page.locator('select[name="sessionEpreuve"]').selectOption(sessionId);
  await page.locator('#searchString').fill(eppn);
  await page.locator('#eppn').evaluate((el, value) => {
    el.value = value;
  }, eppn);
  await page.locator('input[type="submit"][value="Valider"]').click();
}

async function getRowIdByText(page, tableSelector, rowText) {
  const row = page.locator(`${tableSelector} tbody tr`, { hasText: rowText }).first();
  await expect(row).toBeVisible();
  return await row.getAttribute('id');
}

async function addSessionLocation(page, sessionId) {
  await page.goto(`/${CONTEXT_KEY}/manager/sessionLocation?form&sessionEpreuve=${sessionId}`);
  await page.locator('select[name="sessionEpreuve"]').selectOption(sessionId);
  await page.locator('select[name="location"]').selectOption({ label: `${LOCATION_NAME} (10 max)` });
  await page.locator('input[name="capacite"]').fill('10');
  await page.locator('input[name="priorite"]').fill('1');
  await page.locator('input[type="submit"][value="Valider"]').click();
}

async function addTagCheckers(page, sessionId, userIds) {
  await page.goto(`/${CONTEXT_KEY}/manager/tagChecker?form&sessionEpreuve=${sessionId}`);
  await page.locator('select[name="sessionEpreuve"]').selectOption(sessionId);
  await page.locator('select[name="sessionLocation"]').selectOption({ index: 0 });
  for (const userId of userIds) {
    const checkbox = page.locator(`input.sol-checkbox[value="${userId}"]`);
    await checkbox.evaluate((el) => {
      el.checked = true;
      el.dispatchEvent(new Event('change', { bubbles: true }));
    });
  }
  await page.locator('input[type="submit"][value="Valider"]').click();
}

test.describe.configure({ mode: 'serial' });

test('Créer un contexte Playwright complet', async ({ page }) => {
  await loginWithCas(page, ADMIN_USERNAME);

  await createContext(page);
  await createContextAdmin(page);

  await logout(page);

  await loginWithCas(page, USERNAME);
  await page.goto(`/${CONTEXT_KEY}/dashboard`);

  const availableContexts = await page.locator('#selectContext option').evaluateAll((options) =>
    options.map((option) => option.textContent ? option.textContent.trim() : '')
  );
  expect(availableContexts).toContain(CONTEXT_KEY);
  expect(availableContexts).not.toContain('ALL');
  expect(availableContexts).not.toContain('all');

  await unactivateAde(page);

  await page.goto(`/${CONTEXT_KEY}/admin/campus`);
  await createCampus(page);
  await createLocation(page);
  await createSupervisorUser(page, JACK);
  await createSupervisorUser(page, WILLIAM);

  await createSession(page);
  const sessionId = await getRowIdByText(page, '#tableSessionEpreuve', SESSION_NAME);

  await addSessionLocation(page, sessionId);
  await addTagCheck(page, sessionId, 'averell@example.org');
  await addTagCheck(page, sessionId, 'ma@example.org');
  await page.goto(`/${CONTEXT_KEY}/manager/sessionEpreuve`);
  const sessionRow = page.locator(`#tableSessionEpreuve tbody tr[id="${sessionId}"]`);
  await expect(sessionRow.locator('a[href*="/manager/tagCheck/sessionEpreuve/"]')).toHaveText('2');

  await page.goto(`/${CONTEXT_KEY}/admin/userApp`);
  const jackId = await getRowIdByText(page, '#userAppPage table', JACK);
  const williamId = await getRowIdByText(page, '#userAppPage table', WILLIAM);
  await addTagCheckers(page, sessionId, [jackId, williamId]);

  await page.goto(`/${CONTEXT_KEY}/manager/sessionEpreuve`);
  await expect(sessionRow.locator('a[href*="/manager/tagChecker/sessionEpreuve/"]')).toHaveText('2');
});

