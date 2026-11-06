import { test, expect, Page } from '@playwright/test';

const adminUser = process.env.E2E_ADMIN_USER;
const adminPassword = process.env.E2E_ADMIN_PASSWORD;

async function signIn(page: Page, username: string, password: string) {
  await page.locator('input[name="username"]').fill(username);
  await page.locator('input[name="password"]').fill(password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).last().click();
  await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible();
}

test('customer-to-admin ordering and menu edit flow', async ({ browser }, testInfo) => {
  test.skip(!adminUser || !adminPassword, 'E2E_ADMIN_USER and E2E_ADMIN_PASSWORD required');

  const id = Date.now().toString(36);
  const foodName = 'CI Wrap ' + id;
  const updatedName = 'CI Special Wrap ' + id;
  const customerName = 'e2e' + id;
  const customerPass = 'UniqueE2ePassword123!';

  const staffContext = await browser.newContext();
  const customerContext = await browser.newContext();
  const otherContext = await browser.newContext();
  const staff = await staffContext.newPage();
  const customer = await customerContext.newPage();
  const other = await otherContext.newPage();
  try {
    await staff.goto('/');
    await signIn(staff, adminUser!, adminPassword!);
    await staff.getByRole('button', { name: 'Inventory', exact: true }).click();
    await expect(staff.getByRole('heading', { name: 'Menu & inventory' })).toBeVisible();

    await staff.getByRole('button', { name: '+ Add dish' }).click();
    await staff.locator('input[name="foodName"]').fill(foodName);
    await staff.locator('input[name="foodCategory"]').fill('Vegetarian');
    await staff.locator('textarea[name="foodDescription"]').fill('End-to-end test dish');
    await staff.locator('input[name="foodPrice"]').fill('73');
    await staff.locator('input[name="foodStock"]').fill('5');
    await staff.getByRole('button', { name: 'Save dish' }).click();
    const row = staff.locator('.inventory-row').filter({ hasText: foodName });
    await expect(row).toBeVisible();

    await row.getByRole('button', { name: 'Edit', exact: true }).click();
    await staff.locator('input[name="foodName"]').fill(updatedName);
    await staff.locator('input[name="foodPrice"]').fill('79');
    await staff.getByRole('button', { name: 'Save changes' }).click();
    await expect(staff.locator('.inventory-row').filter({ hasText: updatedName })).toBeVisible();
    await staff.screenshot({ path: testInfo.outputPath('admin-inventory.png'), fullPage: true });

    await customer.goto('/');
    await customer.getByRole('button', { name: 'New here? Create an account' }).click();
    await customer.locator('input[name="registerUsername"]').fill(customerName);
    await customer.locator('input[name="registerEmail"]').fill(customerName + '@example.test');
    await customer.locator('input[name="registerName"]').fill('E2E Customer');
    await customer.locator('input[name="registerPassword"]').fill(customerPass);
    await customer.getByRole('button', { name: 'Create account' }).click();
    await expect(customer.getByText('Account created. Sign in to start ordering.')).toBeVisible();
    await signIn(customer, customerName, customerPass);
    await customer.getByLabel('Search menu').fill(updatedName);
    const card = customer.locator('.food-card').filter({ hasText: updatedName });
    await expect(card).toBeVisible();
    await expect(card).toContainText('₹79');
    await card.getByRole('button', { name: 'Add ' + updatedName + ' to cart' }).click();
    await expect(customer.getByText('1 items')).toBeVisible();
    await customer.reload();
    // A per-account local cart survives reload, with the same quantity and price.
    await expect(customer.getByText('1 items')).toBeVisible();
    await expect(customer.locator('.cart-line').filter({ hasText: updatedName })).toContainText('₹79');
    await customer.screenshot({ path: testInfo.outputPath('customer-menu-cart.png'), fullPage: true });
    await customer.getByRole('button', { name: /Place order/ }).click();
    await expect(customer.getByRole('heading', { name: 'My orders' })).toBeVisible();
    const placedOrder = customer.locator('.order-card').filter({ hasText: updatedName }).first();
    await expect(placedOrder).toContainText('PLACED');
    const label = await placedOrder.locator('.order-top small').innerText();
    const orderId = label.replace(/[^0-9]/g, '');
    expect(orderId).toMatch(/^\d+$/);
    await customer.screenshot({ path: testInfo.outputPath('customer-order-placed.png'), fullPage: true });

    // Ownership is enforced by the API, not just by the Angular dashboard.
    await other.goto('/');
    const otherName = customerName + 'b';
    await other.getByRole('button', { name: 'New here? Create an account' }).click();
    await other.locator('input[name="registerUsername"]').fill(otherName);
    await other.locator('input[name="registerEmail"]').fill(otherName + '@example.test');
    await other.locator('input[name="registerPassword"]').fill(customerPass);
    await other.getByRole('button', { name: 'Create account' }).click();
    await signIn(other, otherName, customerPass);
    const otherToken = await other.evaluate(() => sessionStorage.getItem('auth-token'));
    const denied = await other.request.get('/api/orders/' + orderId, {
      headers: { Authorization: 'Bearer ' + otherToken }
    });
    expect(denied.status()).toBe(404);

    await staff.getByRole('button', { name: 'Manage orders' }).click();
    // Completed orders leave the default Active queue; use All for lifecycle assertions.
    await staff.getByLabel('Filter orders').selectOption('ALL');
    const staffOrder = staff.locator('.order-card').filter({ hasText: updatedName }).first();
    await expect(staffOrder).toContainText('PLACED');
    for (const state of ['PREPARING', 'READY', 'COMPLETED']) {
      await staffOrder.locator('.primary-btn').click();
      await expect(staffOrder.locator('.status')).toHaveText(state);
    }
    await staff.screenshot({ path: testInfo.outputPath('admin-completed-order.png'), fullPage: true });
    await customer.getByRole('button', { name: 'Refresh' }).click();
    await expect(placedOrder.locator('.status')).toHaveText('COMPLETED');
    await expect(placedOrder.locator('.timeline-step.reached')).toHaveCount(4);
    await customer.screenshot({ path: testInfo.outputPath('customer-order-complete.png'), fullPage: true });

    await staff.getByRole('button', { name: 'Dashboard' }).click();
    await expect(staff.getByRole('heading', { name: 'Kitchen at a glance' })).toBeVisible();
    await expect(staff.getByText(updatedName).first()).toBeVisible();
    await expect(staff.getByText('Completed order value')).toBeVisible();
    await staff.screenshot({ path: testInfo.outputPath('admin-analytics.png'), fullPage: true });

    await staff.getByRole('button', { name: 'Inventory' }).click();
    await expect(staff.getByRole('heading', { name: 'Inventory movement history' })).toBeVisible();
    await expect(staff.locator('.audit-table tbody tr').first()).toBeVisible();
    await staff.screenshot({ path: testInfo.outputPath('admin-stock-history.png'), fullPage: true });
  } finally {
    await staffContext.close();
    await customerContext.close();
    await otherContext.close();
  }
});

test('mobile storefront is navigable and readable', async ({ browser }, testInfo) => {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 });
  try {
    const page = await context.newPage();
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'What sounds good?' })).toBeVisible();
    await expect(page.locator('.food-card').first()).toBeVisible();
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 2);
    expect(overflow).toBeFalsy();
    await page.screenshot({ path: testInfo.outputPath('mobile-storefront.png'), fullPage: true });
  } finally {
    await context.close();
  }
});
