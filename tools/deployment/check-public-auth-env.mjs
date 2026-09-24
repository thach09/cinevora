import assert from 'node:assert/strict'

const required = (key) => {
  const value = process.env[key]
  assert.ok(value && value.trim(), `${key} is required for the public auth release gate`)
  return value.trim()
}

const frontend = new URL(required('FRONTEND_ORIGIN'))
const backend = new URL(required('BACKEND_ORIGIN'))
for (const [name, url] of [['FRONTEND_ORIGIN', frontend], ['BACKEND_ORIGIN', backend]]) {
  assert.equal(url.protocol, 'https:', `${name} must use HTTPS`)
  assert.equal(url.origin, url.href.replace(/\/$/, ''), `${name} must be an exact origin`)
  assert.ok(!url.username && !url.password, `${name} must not include credentials`)
}
const providerFrontend = frontend.hostname.endsWith('.vercel.app')
const providerBackend = backend.hostname.endsWith('.onrender.com') || backend.hostname.endsWith('.up.railway.app')
assert.ok(!(providerFrontend && providerBackend), 'Separate Vercel/Render/Railway provider sites are unsupported by SameSite=Lax auth cookies')
required('CINEVORA_RELEASE_USERNAME')
required('CINEVORA_RELEASE_PASSWORD')
console.log('Public auth gate inputs present; actual cookie behavior must pass the browser test')
