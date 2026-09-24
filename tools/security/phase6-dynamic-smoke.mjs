const api = process.env.CINEVORA_API_URL || 'http://127.0.0.1:18086/api/v1'
const frontend = process.env.CINEVORA_FRONTEND_URL || 'http://127.0.0.1:18088'
const checks = []
const check = (name, ok, detail) => { checks.push({ name, ok, detail }); if (!ok) throw new Error(`${name}: ${detail}`) }

const health = await fetch(`${api.replace(/\/api\/v1$/, '')}/actuator/health`)
check('minimal health', health.status === 200 && (await health.text()) === '{"status":"UP"}', `${health.status}`)

const csrf = await fetch(`${api}/auth/csrf`)
const csrfBody = await csrf.json()
check('csrf bootstrap', csrf.status === 200 && csrfBody?.data?.token && csrfBody?.data?.headerName, `${csrf.status}`)

const noCsrf = await fetch(`${api}/auth/login`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ username: 'admin', password: 'wrong' }) })
check('missing csrf denied', noCsrf.status === 403, `${noCsrf.status}`)

const hugeQuery = await fetch(`${api}/movies?q=${'x'.repeat(2050)}`)
check('oversized query denied', hugeQuery.status === 414, `${hugeQuery.status}`)

const hugeBody = await fetch(`${api}/auth/login`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ username: 'a'.repeat(70_000) }) })
check('oversized body denied', hugeBody.status === 413, `${hugeBody.status}`)

const cors = await fetch(`${api}/users/me`, { method: 'OPTIONS', headers: { Origin: 'https://attacker.invalid', 'Access-Control-Request-Method': 'GET' } })
check('untrusted cors denied', cors.status === 403, `${cors.status}`)

const front = await fetch(`${frontend}/login`)
check('browser policy headers', front.status === 200 && front.headers.get('content-security-policy')?.includes("frame-ancestors 'none'"), `${front.status}`)

console.log(JSON.stringify(checks))
