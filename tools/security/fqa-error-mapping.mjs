/** Runtime regression for FQA-003. Run only against a disposable dev/CI API. */
import assert from 'node:assert/strict'

const base = (process.env.CINEVORA_TEST_API_URL || 'http://127.0.0.1:18086/api/v1').replace(/\/$/, '')
const credentials = { username: 'admin', password: process.env.CINEVORA_TEST_ADMIN_PASSWORD || 'Cinevora@2026' }
const csrf = await fetch(`${base}/auth/csrf`)
assert.equal(csrf.status, 200)
const proof = (await csrf.json()).data
const cookie = csrf.headers.getSetCookie().find(value => value.startsWith('XSRF-TOKEN='))?.split(';')[0]
const login = await fetch(`${base}/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json', Cookie: cookie, [proof.headerName]: proof.token },
  body: JSON.stringify(credentials),
})
assert.equal(login.status, 200)
const token = (await login.json()).data.token
const bearer = { Authorization: `Bearer ${token}` }
const cases = [
  ['suggestions missing q', `${base}/movies/suggestions`, {}, 400],
  ['search history missing q', `${base}/users/me/search-history`, { method: 'POST', headers: bearer }, 400],
  ['invalid query type', `${base}/movies?minYear=abc`, {}, 400],
  ['malformed JSON', `${base}/users/me/profiles`, { method: 'POST', headers: { ...bearer, 'Content-Type': 'application/json' }, body: '{' }, 400],
  ['unsupported media type', `${base}/users/me/profiles`, { method: 'POST', headers: { ...bearer, 'Content-Type': 'text/plain' }, body: 'name=test' }, 415],
]
const movie = (await fetch(`${base}/movies?size=1`).then(response => response.json())).data.content[0]
const form = new FormData()
form.append('file', new Blob([new Uint8Array(5 * 1024 * 1024 + 1)], { type: 'image/jpeg' }), 'oversized.jpg')
cases.push(['oversized multipart', `${base}/media/movies/${movie.id}/poster`, { method: 'POST', headers: bearer, body: form }, 413])
for (const [name, url, options, expected] of cases) {
  const response = await fetch(url, options)
  const body = await response.json()
  assert.equal(response.status, expected, name)
  assert.equal(body.success, false, name)
  assert.ok(!/exception|stack|sql|java\./i.test(body.message || ''), name)
  console.log(`${name}: ${response.status}, wrapped error`)
}
