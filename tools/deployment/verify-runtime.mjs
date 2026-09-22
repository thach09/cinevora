// Node 24. Test/demo accounts only. Never prints bearer, refresh or reset tokens.
import assert from 'node:assert/strict'
import { mkdir, readFile, writeFile, unlink } from 'node:fs/promises'
import { performance } from 'node:perf_hooks'

const api = process.env.CINEVORA_API_URL || 'http://localhost:18086/api/v1'
const frontend = process.env.CINEVORA_FRONTEND_URL || 'http://localhost:18088'
const local = ['localhost', '127.0.0.1'].includes(new URL(api).hostname)
if (!local && process.env.CINEVORA_ALLOW_QA_MUTATIONS !== 'true') throw new Error('Set CINEVORA_ALLOW_QA_MUTATIONS=true only for an authorized demo/test deployment')
const adminName = process.env.CINEVORA_ADMIN_USERNAME || (local ? 'admin' : '')
const customerName = process.env.CINEVORA_CUSTOMER_USERNAME || (local ? 'thietthach09' : '')
const adminPassword = process.env.CINEVORA_ADMIN_PASSWORD || (local ? 'Cinevora@2026' : '')
const customerPassword = process.env.CINEVORA_CUSTOMER_PASSWORD || (local ? 'Cinevora@2026' : '')
assert(adminName && customerName && adminPassword && customerPassword, 'Supply demo account credentials using environment variables')
const stateFile = process.env.CINEVORA_MEDIA_STATE_FILE || '.tmp/phase5/media-restart-state.json'
const origin = process.env.CINEVORA_ALLOWED_ORIGIN || new URL(frontend).origin

async function call(path, { method = 'GET', token, data, headers = {}, expected = 200, raw = false } = {}) {
  const response = await fetch(path.startsWith('http') ? path : api + path, {
    method, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(data ? { 'Content-Type': 'application/json' } : {}), ...headers },
    body: data ? JSON.stringify(data) : undefined, signal: AbortSignal.timeout(30_000),
  })
  assert.equal(response.status, expected, `${method} ${new URL(response.url).pathname}`)
  if (raw) return response
  const result = await response.json()
  if (expected < 300 && path.startsWith('/')) assert.equal(result.success, true)
  return result.data
}
async function login(username, password) {
  return call('/auth/login', { method: 'POST', data: { username, password } })
}
async function archive(movieId, token) {
  await call(`/admin/movies/${movieId}/status`, { method: 'PATCH', token, data: { active: false } })
}
// A real 1x1 PNG, accepted only if the image decoder validates it.
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3ioAAAAASUVORK5CYII=', 'base64')
async function upload(movieId, token, bytes = png, type = 'image/png', expected = 200) {
  const body = new FormData()
  body.append('file', new Blob([bytes], { type }), '../../untrusted-name.png')
  const response = await fetch(`${api}/media/movies/${movieId}/poster`, { method: 'POST', headers: token ? { Authorization: `Bearer ${token}` } : {}, body, signal: AbortSignal.timeout(30_000) })
  assert.equal(response.status, expected, 'poster upload')
  if (expected !== 200) return
  return (await response.json()).data
}
function publicPoster(url) { return new URL(url, new URL(api).origin).href }

const admin = await login(adminName, adminPassword)
const customer = await login(customerName, customerPassword)
try {
  if (process.argv.includes('--after-restart')) {
    const saved = JSON.parse(await readFile(stateFile, 'utf8'))
    assert.equal(saved.api, api, 'Media state belongs to this target')
    try {
      const movie = await call(`/movies/${saved.movieId}`)
      assert.equal(movie.thumbnailUrl, saved.url)
      assert.equal((await fetch(publicPoster(saved.url))).status, 200)
      const replaced = await upload(saved.movieId, admin.token)
      assert.notEqual(replaced.thumbnailUrl, saved.url)
      assert.equal((await fetch(publicPoster(replaced.thumbnailUrl))).status, 200)
      await call(`/media/movies/${saved.movieId}/poster`, { method: 'DELETE', token: admin.token })
      assert.equal((await call(`/movies/${saved.movieId}`)).thumbnailUrl, null)
      if (local) {
        assert.equal((await fetch(publicPoster(saved.url))).status, 404)
        assert.equal((await fetch(publicPoster(replaced.thumbnailUrl))).status, 404)
      }
      console.log('PASS poster URL + bytes survive backend restart; replacement + removal')
    } finally {
      await archive(saved.movieId, admin.token)
      await unlink(stateFile)
    }
  } else {
    const health = await call(new URL('/actuator/health', api).href, { raw: true })
    assert.deepEqual(await health.json(), { status: 'UP' })
    console.log('PASS health 200, status only')
    await call('/users/me', { expected: 403, raw: true })
    await call('/users/me', { token: 'malformed.jwt', expected: 403, raw: true })
    await call('/statistics', { token: customer.token, expected: 403 })
    await call('/statistics', { token: admin.token })
    const otherProfiles = await call('/users/me/profiles', { token: admin.token })
    await call('/users/me/watchlist', { token: customer.token, headers: { 'X-Profile-Id': String(otherProfiles[0].id) }, expected: 404 })
    const rotated = await call('/auth/refresh', { method: 'POST', data: { refreshToken: customer.refreshToken } })
    await call('/auth/refresh', { method: 'POST', data: { refreshToken: customer.refreshToken }, expected: 400 })
    await call('/auth/logout', { method: 'POST', data: { refreshToken: rotated.refreshToken } })
    await call('/auth/refresh', { method: 'POST', data: { refreshToken: rotated.refreshToken }, expected: 400 })
    console.log('PASS anonymous/malformed JWT=403, CUSTOMER admin=403, ADMIN=200, foreign profile=404, rotated/revoked refresh=400')
    for (const allowed of [true, false]) {
      const cors = await call('/users/me', { method: 'OPTIONS', expected: allowed ? 200 : 403, raw: true, headers: {
        Origin: allowed ? origin : 'https://random-origin.invalid', 'Access-Control-Request-Method': 'GET',
        'Access-Control-Request-Headers': 'Authorization,Content-Type,X-Profile-Id',
      } })
      assert.equal(cors.headers.get('access-control-allow-origin'), allowed ? origin : null)
      if (allowed) assert.equal(cors.headers.get('access-control-allow-credentials'), 'true')
    }
    console.log('PASS exact CORS origin + credentials + profile header; random origin rejected')
    for (const path of ['/', '/login', '/movies/1', '/admin/movies', '/robots.txt', '/favicon.svg'])
      assert.equal((await fetch(frontend + path)).status, 200, path)
    assert.equal((await fetch(frontend + '/assets/does-not-exist.js')).status, 404)
    console.log('PASS SPA deep links/static resources; missing JS=404')
    const movie = await call('/movies', { method: 'POST', token: admin.token, expected: 201, data: {
      title: `Phase5 Media ${Date.now()}`, categoryId: 1, director: 'QA', actors: 'QA', releaseYear: 2026, rating: 8,
    } })
    let prepared = false
    try {
      await upload(movie.id, undefined, png, 'image/png', 403)
      await upload(movie.id, customer.token, png, 'image/png', 403)
      await upload(movie.id, admin.token, Buffer.from('<svg/>'), 'image/png', 400)
      await upload(movie.id, admin.token, png, 'image/jpeg', 400)
      const stored = await upload(movie.id, admin.token)
      assert(stored.thumbnailUrl && !stored.thumbnailUrl.includes('untrusted'))
      assert.equal((await fetch(publicPoster(stored.thumbnailUrl))).status, 200)
      await mkdir('.tmp/phase5', { recursive: true })
      await writeFile(stateFile, JSON.stringify({ api, movieId: movie.id, url: stored.thumbnailUrl }))
      prepared = true
      console.log('PASS poster upload/decoding/role checks; restart fixture prepared')
    } finally { if (!prepared) await archive(movie.id, admin.token) }
    const metrics = []
    for (const [name, path, token] of [['health', new URL('/actuator/health', api).href], ['movies', '/movies'], ['search', '/movies?q=Avengers'], ['home', '/home', customer.token], ['recommendations', '/recommendations', customer.token]]) {
      const samples = []
      for (let i = 0; i < 12; i++) {
        const start = performance.now(); await call(path, { token, raw: name === 'health' }); samples.push(performance.now() - start)
      }
      samples.sort((a, b) => a - b)
      metrics.push({ name, samples: samples.length, p50: +samples[5].toFixed(2), p95: +samples[11].toFixed(2), max: +samples.at(-1).toFixed(2) })
    }
    const loginTimes = []
    for (let i = 0; i < 6; i++) {
      const start = performance.now(); const session = await login(customerName, customerPassword); loginTimes.push(performance.now() - start)
      await call('/auth/logout', { method: 'POST', data: { refreshToken: session.refreshToken } })
    }
    loginTimes.sort((a, b) => a - b)
    metrics.push({ name: 'login', samples: 6, p50: +loginTimes[2].toFixed(2), p95: +loginTimes[5].toFixed(2), max: +loginTimes[5].toFixed(2) })
    console.log(JSON.stringify({ environment: local ? 'local (not cloud SLA)' : 'deployed warm', unit: 'ms', metrics }, null, 2))
  }
} finally {
  for (const auth of [admin, customer]) await call('/auth/logout', { method: 'POST', data: { refreshToken: auth.refreshToken } })
}
