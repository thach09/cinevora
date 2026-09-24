/** Keep first-boot requirements visible in provider and operator configuration. */
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const blueprint = readFileSync('render.yaml', 'utf8')
const runbook = readFileSync('docs/deployment/DEPLOYMENT_RUNBOOK.md', 'utf8')
const template = readFileSync('.env.example', 'utf8')
for (const key of ['BOOTSTRAP_ADMIN_USERNAME', 'BOOTSTRAP_ADMIN_EMAIL', 'BOOTSTRAP_ADMIN_PASSWORD']) {
  assert.match(blueprint, new RegExp(`- key: ${key}\\s+sync: false`), `${key} must be an external Render secret`)
  assert.ok(runbook.includes(key), `${key} missing from runbook`)
  assert.ok(template.includes(key), `${key} missing from environment template`)
  console.log(`${key}: provider external secret, runbook and template present`)
}
