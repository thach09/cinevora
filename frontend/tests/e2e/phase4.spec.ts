import { expect, test, type APIRequestContext, type Page, type TestInfo } from '@playwright/test'

const API_BASE = process.env.CINEVORA_API_URL || 'http://localhost:8080/api/v1'
const DEMO_PASSWORD = 'Cinevora@2026'
const ADMIN = { username: 'admin', password: DEMO_PASSWORD }
const CUSTOMER = { username: 'thietthach09', password: DEMO_PASSWORD }

type Diagnostics = {
  consoleErrors: string[]
  pageErrors: string[]
  requestFailures: string[]
  serverErrors: string[]
}

function watchDiagnostics(page: Page): Diagnostics {
  const diagnostics: Diagnostics = { consoleErrors: [], pageErrors: [], requestFailures: [], serverErrors: [] }
  page.on('console', (message) => { if (message.type() === 'error') diagnostics.consoleErrors.push(message.text()) })
  page.on('pageerror', (error) => diagnostics.pageErrors.push(error.message))
  page.on('requestfailed', (request) => diagnostics.requestFailures.push(`${request.method()} ${request.url()} — ${request.failure()?.errorText || 'failed'}`))
  page.on('response', (response) => { if (response.status() >= 500) diagnostics.serverErrors.push(`${response.status()} ${response.url()}`) })
  return diagnostics
}

async function expectNoDiagnostics(diagnostics: Diagnostics) {
  expect(diagnostics.consoleErrors, `browser console errors: ${diagnostics.consoleErrors.join('; ')}`).toEqual([])
  expect(diagnostics.pageErrors, `page errors: ${diagnostics.pageErrors.join('; ')}`).toEqual([])
  expect(diagnostics.requestFailures, `failed requests: ${diagnostics.requestFailures.join('; ')}`).toEqual([])
  expect(diagnostics.serverErrors, `server errors: ${diagnostics.serverErrors.join('; ')}`).toEqual([])
}

async function expectToast(page: Page, message: string | RegExp) {
  await expect(page.getByRole('status').filter({ hasText: message })).toBeVisible()
}

async function login(page: Page, username: string, password: string) {
  await page.goto('/login')
  await page.getByPlaceholder('your username').fill(username)
  await page.getByPlaceholder('••••••••').fill(password)
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
  await page.waitForURL(username === ADMIN.username ? '**/admin' : '**/browse')
  await expect(page.getByRole('button', { name: /Sign out|Exit/ })).toBeVisible()
}

async function logout(page: Page) {
  await page.getByRole('button', { name: 'Sign out', exact: true }).click()
  await page.waitForURL('**/login')
}

async function apiLogin(request: APIRequestContext, credentials: { username: string; password: string }) {
  const response = await request.post(`${API_BASE}/auth/login`, { data: credentials })
  expect(response.ok()).toBeTruthy()
  return (await response.json()).data as { token: string }
}

async function cleanupAdminContent(request: APIRequestContext, movieId?: number, categoryId?: number) {
  const auth = await apiLogin(request, ADMIN)
  const headers = { Authorization: `Bearer ${auth.token}` }
  if (movieId) await request.patch(`${API_BASE}/admin/movies/${movieId}/status`, { headers, data: { active: false } })
  if (categoryId) await request.patch(`${API_BASE}/admin/categories/${categoryId}/status`, { headers, data: { active: false } })
}

async function createMovieFixture(request: APIRequestContext, title: string, categoryId: number, trailerUrl?: string) {
  const auth = await apiLogin(request, ADMIN)
  const response = await request.post(`${API_BASE}/movies`, {
    headers: { Authorization: `Bearer ${auth.token}` },
    data: {
      title,
      categoryId,
      director: 'Phase 4 Director',
      actors: 'Phase 4 Cast',
      releaseYear: 2026,
      rating: 8.8,
      durationMinutes: 95,
      videoUrl: 'data:video/mp4;base64,AAAA',
      trailerUrl,
      description: 'Created by the Phase 4 browser integration suite.',
    },
  })
  expect(response.status()).toBe(201)
  return (await response.json()).data.id as number
}

test.describe('Cinevora Phase 4 final integration', () => {
  test.beforeEach(async ({ page }) => {
    // Exercise our iframe lifecycle without depending on third-party ads/fonts/network.
    // Real trailer playback remains a separate deployed-browser verification gate.
    await page.route('https://www.youtube-nocookie.com/embed/**', (route) => route.fulfill({
      contentType: 'text/html', body: '<!doctype html><title>Trailer test fixture</title>',
    }))
  })

  test('customer registration, browse, detail and personal library journey', async ({ page }) => {
    const diagnostics = watchDiagnostics(page)
    const stamp = Date.now()
    await page.goto('/register')
    await page.getByPlaceholder('Alex Morgan').fill('Phase Four Customer')
    await page.getByPlaceholder('alexmorgan').fill(`phase4_${stamp}`)
    await page.getByPlaceholder('alex@example.com').fill(`phase4_${stamp}@example.test`)
    await page.getByPlaceholder('At least 8 characters').fill('Cinevora@2026')
    await page.getByRole('button', { name: 'Create account', exact: true }).click()
    await page.waitForURL('**/browse')
    await expect(page.getByRole('heading', { name: 'A little something for everyone' })).toBeVisible()

    const firstCard = page.locator('a.movie-card').first()
    await expect(firstCard).toBeVisible()
    const movieTitle = (await firstCard.locator('h3').innerText()).trim()
    await firstCard.click()
    await page.waitForURL('**/movies/*')
    await expect(page.locator('.detail-copy h2')).toHaveText(movieTitle)
    await page.getByRole('button', { name: /Add to watchlist/ }).click()
    await expectToast(page, 'Updated your watchlist.')
    await page.getByRole('button', { name: 'Favourite', exact: true }).click()
    await expectToast(page, 'Updated your favourites.')
    await page.getByRole('button', { name: 'Mark as watched', exact: true }).click()
    await expectToast(page, 'Added to your watch history.')

    await page.locator('a.side-link').filter({ hasText: 'Watchlist' }).click()
    await expect(page.getByRole('heading', { name: 'Your watchlist' })).toBeVisible()
    await expect(page.getByText(movieTitle, { exact: true })).toBeVisible()
    await page.locator('a.side-link').filter({ hasText: 'Favourites' }).click()
    await expect(page.getByRole('heading', { name: 'Your favourites' })).toBeVisible()
    await expect(page.getByText(movieTitle, { exact: true })).toBeVisible()
    await page.locator('a.side-link').filter({ hasText: 'History' }).click()
    await expect(page.getByRole('heading', { name: 'Watch history' })).toBeVisible()
    await expect(page.getByText(movieTitle, { exact: true })).toBeVisible()
    await expectNoDiagnostics(diagnostics)
  })

  test('admin CMS journey and category business-rule rejection', async ({ page, request }) => {
    const diagnostics = watchDiagnostics(page)
    const stamp = Date.now()
    const categoryName = `Phase 4 Category ${stamp}`
    const movieTitle = `Phase 4 Movie ${stamp}`
    let categoryId: number | undefined
    let movieId: number | undefined
    try {
      await login(page, ADMIN.username, ADMIN.password)
      await page.goto('/admin/categories')
      await expect(page.locator('main h2')).toHaveText('Categories')
      const categoryResponsePromise = page.waitForResponse((response) => response.request().method() === 'POST' && response.url().endsWith('/api/v1/categories'))
      await page.getByLabel('Name', { exact: true }).fill(categoryName)
      await page.getByPlaceholder('A short description').fill('Created by the Phase 4 browser integration suite.')
      await page.getByRole('button', { name: 'Create category', exact: true }).click()
      const categoryResponse = await categoryResponsePromise
      expect(categoryResponse.status()).toBe(200)
      categoryId = (await categoryResponse.json()).data.id
      await expectToast(page, 'Category created.')

      await page.goto('/admin/movies')
      await expect(page.locator('main h2')).toHaveText('Movies')
      await expect(page.locator('input[name="trailerUrl"]')).toBeVisible()
      await expect(page.locator('input[name="videoUrl"]')).toBeVisible()
      movieId = await createMovieFixture(request, movieTitle, categoryId!)

      await page.goto('/admin/categories')
      const categoryRow = page.locator('.table-row').filter({ hasText: categoryName })
      await expect(categoryRow).toBeVisible()
      const archiveResponsePromise = page.waitForResponse((response) => response.request().method() === 'PATCH' && response.url().includes(`/api/v1/admin/categories/${categoryId}/status`))
      await categoryRow.getByRole('button', { name: 'Archive', exact: true }).click()
      expect((await archiveResponsePromise).status()).toBe(400)
      await expect(page.getByRole('status').last()).toContainText(/thể loại|phim|category/i)
      const expected400Index = diagnostics.consoleErrors.findIndex((message) => message.includes('400'))
      if (expected400Index >= 0) diagnostics.consoleErrors.splice(expected400Index, 1)
      await expect(categoryRow.getByRole('button', { name: 'Archive', exact: true })).toBeVisible()
      await expectNoDiagnostics(diagnostics)
    } finally {
      await cleanupAdminContent(request, movieId, categoryId)
    }
  })

  test('cross-role publish, customer discovery/play, and admin statistics', async ({ page, request }) => {
    test.setTimeout(60_000)
    const diagnostics = watchDiagnostics(page)
    const stamp = Date.now()
    const categoryName = `Cross Role Category ${stamp}`
    const movieTitle = `Cross Role Movie ${stamp}`
    let categoryId: number | undefined
    let movieId: number | undefined
    try {
      await login(page, ADMIN.username, ADMIN.password)
      await page.goto('/admin/statistics')
      await expect(page.locator('.stat-card').filter({ hasText: 'Total views' })).toBeVisible()
      const beforeStatsAuth = await apiLogin(request, ADMIN)
      const beforeStats = (await (await request.get(`${API_BASE}/statistics`, { headers: { Authorization: `Bearer ${beforeStatsAuth.token}` } })).json()).data as { totalViews: number }

      await page.goto('/admin/categories')
      const categoryResponsePromise = page.waitForResponse((response) => response.request().method() === 'POST' && response.url().endsWith('/api/v1/categories'))
      await page.getByLabel('Name', { exact: true }).fill(categoryName)
      await page.getByRole('button', { name: 'Create category', exact: true }).click()
      categoryId = (await (await categoryResponsePromise).json()).data.id

      await page.goto('/admin/movies')
      await expect(page.locator('input[name="trailerUrl"]')).toBeVisible()
      await expect(page.locator('input[name="videoUrl"]')).toBeVisible()
      movieId = await createMovieFixture(request, movieTitle, 1, 'https://www.youtube.com/watch?v=JfVOs4VSpmA')

      await logout(page)
      await login(page, CUSTOMER.username, CUSTOMER.password)
      await page.goto(`/search?q=${encodeURIComponent(movieTitle)}`)
      const movieCard = page.locator('a.movie-card').filter({ hasText: movieTitle }).first()
      await expect(movieCard).toBeVisible()
      await movieCard.click()
      await expect(page.locator('.detail-copy h2')).toHaveText(movieTitle)
      const trailerPersistenceCalls: string[] = []
      page.on('request', (request) => {
        const isProgress = request.method() === 'PUT' && request.url().includes('/continue-watching')
        const isHistoryWrite = request.method() === 'POST' && request.url().includes('/history/')
        if (isProgress || isHistoryWrite) trailerPersistenceCalls.push(`${request.method()} ${request.url()}`)
      })
      await page.getByRole('button', { name: 'Watch Trailer', exact: true }).click()
      await expect(page.locator('iframe[title="Official movie trailer"]')).toBeVisible()
      await page.waitForTimeout(500)
      expect(trailerPersistenceCalls, 'trailer playback must not write progress or history').toEqual([])
      await page.getByRole('button', { name: 'Close player', exact: true }).click()
      await page.getByRole('button', { name: 'Watch Now', exact: true }).click()
      await expect(page.locator('.video-player')).toBeVisible()
      await expect(page.locator('video')).toBeVisible()
      await page.getByRole('button', { name: 'Mark as watched', exact: true }).click()
      await expectToast(page, 'Added to your watch history.')

      await logout(page)
      await login(page, ADMIN.username, ADMIN.password)
      await page.goto('/admin/statistics')
      await expect(page.locator('.stat-card').filter({ hasText: 'Total views' })).toBeVisible()
      const afterStatsAuth = await apiLogin(request, ADMIN)
      const afterStats = (await (await request.get(`${API_BASE}/statistics`, { headers: { Authorization: `Bearer ${afterStatsAuth.token}` } })).json()).data as { totalViews: number }
      expect(afterStats.totalViews).toBeGreaterThanOrEqual(beforeStats.totalViews + 1)
      const expectedTrailerAbort = diagnostics.requestFailures.findIndex((message) => message.includes('youtube-nocookie.com/embed/'))
      if (expectedTrailerAbort >= 0) diagnostics.requestFailures.splice(expectedTrailerAbort, 1)
      await expectNoDiagnostics(diagnostics)
    } finally {
      await cleanupAdminContent(request, movieId, categoryId)
    }
  })

  test('profile switching never shows another profile’s watchlist cache', async ({ page, request }) => {
    const diagnostics = watchDiagnostics(page)
    const profileName = `Phase 4 Profile ${Date.now()}`
    try {
      await login(page, CUSTOMER.username, CUSTOMER.password)
      await page.goto('/account')
      const profileSelect = page.locator('select.profile-select')
      await expect(profileSelect).toBeVisible()
      const defaultProfileId = await profileSelect.inputValue()
      await page.getByLabel('New profile name', { exact: true }).fill(profileName)
      await page.getByRole('button', { name: 'Add', exact: true }).click()
      await expectToast(page, 'Profile created.')
      const newProfileOption = profileSelect.locator('option').filter({ hasText: profileName })
      await expect(newProfileOption).toHaveCount(1)
      await profileSelect.selectOption({ label: profileName })
      await expect(profileSelect).not.toHaveValue(defaultProfileId)

      await page.goto('/movies/1')
      await expect(page.getByRole('heading', { name: 'Avengers: Endgame', exact: true })).toBeVisible()
      await page.getByRole('button', { name: /Add to watchlist/ }).click()
      await expectToast(page, 'Updated your watchlist.')
      await page.goto('/watchlist')
      await expect(page.getByText('Avengers: Endgame', { exact: true })).toBeVisible()

      await profileSelect.selectOption(defaultProfileId)
      await expect(page.getByText('Avengers: Endgame', { exact: true })).not.toBeVisible()
      await expectNoDiagnostics(diagnostics)
    } finally {
      const auth = await apiLogin(request, CUSTOMER)
      const headers = { Authorization: `Bearer ${auth.token}` }
      const profilesResponse = await request.get(`${API_BASE}/users/me/profiles`, { headers })
      const profiles = (await profilesResponse.json()).data as Array<{ id: number; name: string; defaultProfile: boolean }>
      const created = profiles.find((profile) => profile.name === profileName && !profile.defaultProfile)
      if (created) await request.delete(`${API_BASE}/users/me/profiles/${created.id}`, { headers })
    }
  })

  test('console/network health and responsive layouts at 375/768/1280/1440', async ({ page }, testInfo: TestInfo) => {
    const diagnostics = watchDiagnostics(page)
    await login(page, CUSTOMER.username, CUSTOMER.password)
    for (const width of [375, 768, 1280, 1440]) {
      await page.setViewportSize({ width, height: 900 })
      await page.goto('/browse')
      await expect(page.locator('#main-content')).toBeVisible()
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1)
      expect(overflow, `horizontal overflow at ${width}px`).toBe(false)
      await page.screenshot({ path: testInfo.outputPath(`responsive-${width}.png`), fullPage: true })
      if (width < 768) {
        await page.getByRole('button', { name: 'Open menu', exact: true }).click()
        await expect(page.getByRole('button', { name: 'Close menu', exact: true }).first()).toBeVisible()
        await page.getByRole('button', { name: 'Close menu', exact: true }).first().click()
      } else {
        await expect(page.getByRole('button', { name: 'Open menu', exact: true })).toBeHidden()
      }
    }
    await expectNoDiagnostics(diagnostics)
  })
})
