import { expect, test } from '@playwright/test'

const password = 'Cinevora@2026'

async function login(page: import('@playwright/test').Page, username: string) {
  await page.goto('/login')
  await page.getByPlaceholder('your username').fill(username)
  await page.getByPlaceholder('••••••••').fill(password)
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  await page.waitForURL(username === 'admin' ? '**/admin' : '**/browse')
}

test('admin can send an individual in-app message that appears only in the customer inbox', async ({ page }) => {
  const title = `E2E private notice ${Date.now()}`
  await login(page, 'admin')
  await page.goto('/admin/notifications')
  await expect(page.getByRole('heading', { name: 'Send notifications' })).toBeVisible()
  await page.getByLabel('Recipient').selectOption('one')
  await page.getByLabel('Choose a customer').selectOption('thietthach09')
  await page.getByLabel('Title').fill(title)
  await page.getByLabel('Message').fill('This message is delivered through the Cinevora inbox.')
  await page.getByLabel('Optional internal link').fill('/browse')
  await page.getByRole('button', { name: 'Send notification', exact: true }).click()
  await expect(page.getByRole('status').filter({ hasText: 'Notification sent to 1 recipient(s).' })).toBeVisible()

  await page.getByRole('button', { name: 'Sign out', exact: true }).click()
  await page.waitForURL('**/login')
  await login(page, 'thietthach09')
  await page.getByRole('button', { name: /Notifications/ }).click()
  await expect(page.getByText(title, { exact: true })).toBeVisible()
})
