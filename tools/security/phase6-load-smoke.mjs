const base = process.env.CINEVORA_API_URL || 'http://127.0.0.1:18086/api/v1'
const total = Number(process.env.CINEVORA_LOAD_REQUESTS || 125)
const started = performance.now()
const samples = await Promise.all(Array.from({ length: total }, async (_, i) => {
  const at = performance.now()
  const response = await fetch(`${base}/movies?q=load-${i}`)
  return { status: response.status, ms: performance.now() - at }
}))
const elapsed = performance.now() - started
const counts = Object.fromEntries([...new Set(samples.map(sample => sample.status))].map(status => [status, samples.filter(sample => sample.status === status).length]))
const sorted = samples.map(sample => sample.ms).sort((a, b) => a - b)
const p95 = sorted[Math.min(sorted.length - 1, Math.floor(sorted.length * 0.95))]
console.log(JSON.stringify({ total, counts, p95Ms: Math.round(p95), elapsedMs: Math.round(elapsed) }))
if (!counts[200] || !counts[429]) process.exitCode = 1
