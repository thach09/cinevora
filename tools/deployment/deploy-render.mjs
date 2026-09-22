// Trigger the tested commit, then wait for that exact Render deployment to become live.
import assert from 'node:assert/strict'
import { setTimeout as delay } from 'node:timers/promises'

const required = name => { const value = process.env[name]; assert(value, `Missing ${name}`); return value }
const key = required('RENDER_API_KEY')
const service = required('RENDER_SERVICE_ID')
const commit = required('RELEASE_SHA')
assert.match(commit, /^[0-9a-f]{40}$/)
assert.match(service, /^srv-[a-z0-9]+$/)
const base = `https://api.render.com/v1/services/${service}/deploys`
async function render(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' }, signal: AbortSignal.timeout(30_000) })
  assert(response.ok, `Render API returned HTTP ${response.status}`)
  return response.json()
}
const deployment = await render(base, { method: 'POST', body: JSON.stringify({ commitId: commit, clearCache: 'do_not_clear' }) })
assert.match(deployment.id, /^dep-[a-z0-9]+$/)
let live = false
for (let attempt = 0; attempt < 90; attempt++) {
  const result = await render(`${base}/${deployment.id}`)
  console.log(`Render deployment ${deployment.id}: ${result.status}`)
  if (result.status === 'live') { assert.equal(result.commit?.id, commit); live = true; break }
  assert(!['build_failed', 'update_failed', 'canceled', 'deactivated', 'pre_deploy_failed'].includes(result.status), 'Render deployment failed')
  await delay(10_000)
}
assert(live, 'Render did not become live within 15 minutes')
const origin = new URL(required('BACKEND_ORIGIN'))
assert.equal(origin.protocol, 'https:')
const response = await fetch(new URL('/actuator/health', origin), { signal: AbortSignal.timeout(90_000) })
assert.equal(response.status, 200)
assert.deepEqual(await response.json(), { status: 'UP' })
console.log(`PASS backend commit ${commit} live, health UP`)
