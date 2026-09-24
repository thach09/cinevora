import { csrfPost } from './security-helpers'
import { expect, test, type APIRequestContext, type Page } from '@playwright/test'

const API_BASE = process.env.CINEVORA_API_URL || 'http://localhost:8080/api/v1'
const DEMO_PASSWORD = 'Cinevora@2026'
const ADMIN = { username: 'admin', password: DEMO_PASSWORD }
const CUSTOMER = { username: 'thietthach09', password: DEMO_PASSWORD }

async function login(request: APIRequestContext, credentials: { username: string; password: string }) {
  const response = await csrfPost(request, `${API_BASE}/auth/login`, { data: credentials })
  expect(response.ok()).toBeTruthy()
  return (await response.json()).data as { token: string }
}

async function loginInBrowser(page: Page, credentials: { username: string; password: string }) {
  await page.goto('/login')
  await page.getByPlaceholder('your username').fill(credentials.username)
  await page.locator('input[type="password"]').fill(credentials.password)
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  await page.waitForURL('**/browse')
}

test('trailer playback is separate from Watch Now persistence', async ({ page, request }) => {
  await page.route('https://www.youtube-nocookie.com/embed/**', (route) => route.fulfill({
    contentType: 'text/html', body: '<!doctype html><title>Trailer test fixture</title>',
  }))
  await page.route('**/media/security-test-video.mp4', (route) => route.fulfill({
    status: 200, contentType: 'video/mp4', body: Buffer.alloc(0),
  }))
  const adminAuth = await login(request, ADMIN)
  const headers = { Authorization: `Bearer ${adminAuth.token}` }
  const title = `Playback Separation ${Date.now()}`
  let movieId: number | undefined

  try {
    const create = await request.post(`${API_BASE}/movies`, {
      headers,
      data: {
        title,
        categoryId: 1,
        director: 'Playback Test Director',
        actors: 'Playback Test Cast',
        releaseYear: 2026,
        rating: 8.5,
        durationMinutes: 95,
        videoUrl: '/media/security-test-video.mp4',
        trailerUrl: 'https://www.youtube.com/watch?v=JfVOs4VSpmA',
        thumbnailUrl: 'https://upload.wikimedia.org/wikipedia/commons/8/8a/Avengers_Endgame_logo.svg',
        description: 'Automated playback separation test fixture.',
      },
    })
    expect(create.status()).toBe(201)
    movieId = (await create.json()).data.id

    await loginInBrowser(page, CUSTOMER)
    await page.goto(`/movies/${movieId}`)
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible()

    const persistenceCalls: string[] = []
    page.on('request', (requestEvent) => {
      const progress = requestEvent.method() === 'PUT' && requestEvent.url().includes('/continue-watching')
      const history = requestEvent.method() === 'POST' && requestEvent.url().includes('/history/')
      if (progress || history) persistenceCalls.push(`${requestEvent.method()} ${requestEvent.url()}`)
    })

    await page.getByRole('button', { name: 'Watch Trailer', exact: true }).click()
    await expect(page.locator('iframe[title="Official movie trailer"]')).toBeVisible()
    await page.waitForTimeout(500)
    expect(persistenceCalls, 'TRAILER must not write progress or history').toEqual([])

    await page.getByRole('button', { name: 'Close player', exact: true }).click()
    await page.getByRole('button', { name: 'Watch Now', exact: true }).click()
    await expect(page.locator('video')).toBeVisible()
  } finally {
    if (movieId) await request.patch(`${API_BASE}/admin/movies/${movieId}/status`, { headers, data: { active: false } })
  }
})
