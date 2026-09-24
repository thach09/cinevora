import { csrfPost } from './security-helpers'
import { expect, test } from '@playwright/test'

const API = process.env.CINEVORA_API_URL || 'http://localhost:18086/api/v1'
const local = ['localhost', '127.0.0.1'].includes(new URL(API).hostname)
const admin = { username: process.env.CINEVORA_ADMIN_USERNAME || 'admin', password: process.env.CINEVORA_ADMIN_PASSWORD || 'Cinevora@2026' }
const customer = { username: process.env.CINEVORA_CUSTOMER_USERNAME || 'thietthach09', password: process.env.CINEVORA_CUSTOMER_PASSWORD || 'Cinevora@2026' }

test('poster upload is visible to customers after reload, replacement and removal', async ({ page, request }) => {
  test.skip(!local && process.env.CINEVORA_ALLOW_QA_MUTATIONS !== 'true', 'Explicit demo deployment required')
  const auth = (await (await csrfPost(request, `${API}/auth/login`, { data: admin })).json()).data
  const headers = { Authorization: `Bearer ${auth.token}` }
  const title = `Phase5 Poster ${Date.now()}`
  const created = await request.post(`${API}/movies`, { headers, data: { title, categoryId: 1, director: 'QA', actors: 'QA', releaseYear: 2026, rating: 8 } })
  expect(created.status()).toBe(201)
  const movieId = (await created.json()).data.id
  try {
    await page.goto('/login')
    await page.getByPlaceholder('your username').fill(admin.username)
    await page.locator('input[type="password"]').fill(admin.password)
    await page.getByRole('button', { name: 'Sign in', exact: true }).click()
    await page.waitForURL('**/admin')
    await page.goto('/admin/movies')
    const card = page.locator('a.movie-card').filter({ hasText: title })
    await expect(page.locator('a.movie-card').first()).toBeVisible()
    for (let pageIndex = 0; pageIndex < 30 && !(await card.isVisible()); pageIndex++) {
      const next = page.getByRole('button', { name: 'Next', exact: true })
      if (!(await next.isVisible()) || !(await next.isEnabled())) break
      await Promise.all([
        page.waitForResponse(response => response.url().includes('/api/v1/movies?') && response.request().method() === 'GET'),
        next.click(),
      ])
      await expect(page.locator('a.movie-card').first()).toBeVisible()
    }
    await expect(card).toBeVisible()
    await card.locator('..').getByRole('button', { name: 'Edit', exact: true }).click()
    const dataUrl = await page.evaluate(() => {
      const canvas = document.createElement('canvas'); canvas.width = 32; canvas.height = 48
      const context = canvas.getContext('2d')!; context.fillStyle = '#c02080'; context.fillRect(0, 0, 32, 48)
      return canvas.toDataURL('image/webp')
    })
    const image = Buffer.from(dataUrl.split(',')[1], 'base64')
    await page.locator('input[type="file"]').setInputFiles({ name: 'poster.webp', mimeType: 'image/webp', buffer: image })
    await page.getByRole('button', { name: 'Upload poster', exact: true }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Poster uploaded.' })).toBeVisible()
    await expect(page.getByAltText('Poster preview')).toHaveJSProperty('naturalWidth', 32)
    const movie = (await (await request.get(`${API}/movies/${movieId}`)).json()).data
    expect(movie.thumbnailUrl).toMatch(/\/posters\/[a-f0-9-]+\.webp$/)

    await page.getByRole('button', { name: 'Sign out', exact: true }).click()
    await page.getByPlaceholder('your username').fill(customer.username)
    await page.locator('input[type="password"]').fill(customer.password)
    await page.getByRole('button', { name: 'Sign in', exact: true }).click()
    await page.waitForURL('**/browse')
    await page.goto(`/search?q=${encodeURIComponent(title)}`)
    await expect(page.locator('a.movie-card').filter({ hasText: title }).locator('img')).toHaveJSProperty('naturalWidth', 32)
    await page.goto(`/movies/${movieId}`)
    await expect(page.getByAltText(`${title} poster`)).toHaveJSProperty('naturalWidth', 32)
    await page.reload()
    await expect(page.getByAltText(`${title} poster`)).toHaveJSProperty('naturalWidth', 32)
    const replace = await request.post(`${API}/media/movies/${movieId}/poster`, { headers, multipart: { file: { name: 'replace.webp', mimeType: 'image/webp', buffer: image } } })
    expect(replace.status()).toBe(200)
    expect((await replace.json()).data.thumbnailUrl).not.toBe(movie.thumbnailUrl)
    await page.reload()
    await expect(page.getByAltText(`${title} poster`)).toHaveJSProperty('naturalWidth', 32)
    expect((await request.delete(`${API}/media/movies/${movieId}/poster`, { headers })).status()).toBe(200)
    await page.reload()
    await expect(page.getByAltText(`${title} poster`)).toHaveCount(0)
  } finally {
    await request.delete(`${API}/media/movies/${movieId}/poster`, { headers })
    await request.patch(`${API}/admin/movies/${movieId}/status`, { headers, data: { active: false } })
    await csrfPost(request, `${API}/auth/logout`)
  }
})
