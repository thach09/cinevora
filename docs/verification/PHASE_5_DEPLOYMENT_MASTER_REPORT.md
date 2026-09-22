# Cinevora Phase 5 deployment master report

**Date:** 2026-09-22
**Commit tested at start:** `6d6db387e5851cb6916749fe9385745e4ec76127`
**Cloud account:** not connected; no cloud deployment or push performed

## Scope and working tree

The Phase 4 trailer/Watch Now changes and migration V9 were preserved. No reset, clean, destructive database operation or `docker compose down -v` was used. Local evidence was recorded under the ignored `.tmp/phase5/` directory.

## Implemented

- Reproducible multi-stage backend and frontend images with pinned base-image digests, non-root backend, health checks, graceful shutdown and Nginx SPA fallback.
- Full-stack Compose with PostgreSQL, backend, frontend, isolated volumes, service health ordering and the Flyway tools profile.
- Production guardrails for `prod`, strong JWT, exact HTTPS CORS, no development tokens, no verbose logging, disabled Swagger/OpenAPI and persistent S3-compatible media.
- Local media validation plus Cloudflare R2/S3-compatible storage adapter. Media cleanup runs after a successful database transaction.
- GitHub Actions backend, frontend, integration and gated deploy workflows. Deploy waits for the exact Render commit and backend health before publishing the frontend.
- Render Blueprint and Vercel SPA configuration; environment templates contain names and safe local values only.
- Runtime smoke, browser E2E, poster persistence/restart verification, local performance sampling, Lighthouse, actionlint and Gitleaks checks.

## Commands and raw results

```text
backend: mvn -B -ntp clean test
Tests run: 20, Failures: 0, Errors: 0
BUILD SUCCESS

backend: mvn -B -ntp package
BUILD SUCCESS

frontend: npm audit --audit-level=high
found 0 vulnerabilities

frontend: npm run lint; npm exec tsc -- --noEmit --pretty false; npm run build
all PASS; vite v6.4.3; 174 modules transformed; built in 1.60s

fresh database: Flyway V1 through V9
Successfully applied 9 migrations to schema "public", now at version v9

Compose: docker compose ... up --build -d --wait
postgres healthy; backend healthy; frontend healthy

Playwright: npm run e2e
7 passed (27.2s)

runtime smoke: node tools/deployment/verify-runtime.mjs
health; JWT/role/profile/session; CORS; SPA/static; poster authorization and upload PASS
local measurements: health p95 6.88 ms; movies 31.17 ms; search 10.09 ms;
home 78.68 ms; recommendations 30.96 ms; login 61.09 ms

restart smoke: node tools/deployment/verify-runtime.mjs --after-restart
PASS poster URL + bytes survive backend restart; replacement + removal

Lighthouse against Compose frontend
Performance 85; Accessibility 100; Best Practices 100; SEO 100; LCP 3.5 s; CLS 0

Lighthouse against local Vite preview
Performance 99; Accessibility 100; Best Practices 100; SEO 100

actionlint
exit 0

gitleaks git: 140 commits scanned; no leaks found; exit 0
gitleaks working tree: no leaks found; exit 0

render.yaml schema validation
render.yaml schema valid=true
```

The Compose Lighthouse score is lower than the Vite preview because it includes container proxy and local cold-start behavior; it is a local measurement, not a cloud SLA.

## Security and storage evidence

The production Spring context was run against an isolated `cinevora_ci` PostgreSQL database with explicit prod variables. It applied all nine migrations and passed three runtime tests: health output is minimal, actuator internals and Swagger are unavailable, malformed/anonymous access is rejected, exact CORS is allowed while a random origin is rejected, and password-reset responses do not include development tokens.

Poster tests validate decoded JPG/PNG/WEBP bytes, MIME, dimensions, size, generated UUID keys, role authorization and S3 signature behavior. The Compose browser suite then verified customer visibility, reload, replacement and removal; the restart smoke verified media remained available after backend restart.

## Hosting decision

The selected evaluation topology is Render Free PostgreSQL + Render Docker Web Service + Vercel Hobby + Cloudflare R2. Render free services sleep and its free PostgreSQL expires after 30 days; therefore this is a free review deployment path, not a durable production availability guarantee. The decision and migration path are in [`ADR_PHASE_5_HOSTING.md`](../adr/ADR_PHASE_5_HOSTING.md).

## Deployment status

| Gate | Result | Evidence |
|---|---|---|
| Backend Docker | PASS | `docker-release-build.log`; image built and backend healthy |
| Frontend Docker | PASS | same Compose build; frontend healthy |
| Full-stack Compose | PASS | three services healthy; isolated CI env |
| CI workflows | PASS (static) | actionlint exit 0; not executed on GitHub because no push was authorized |
| Clean Flyway V1–V9 | PASS | isolated PostgreSQL, version v9 |
| Production profile guard | PASS | 20 backend tests including prod runtime test |
| Managed PostgreSQL cloud | NOT DEPLOYED | no provider account/credentials |
| Backend cloud | NOT DEPLOYED | no Render/Railway account/credentials |
| Frontend cloud | NOT DEPLOYED | no Vercel account/token |
| Persistent media cloud | NOT DEPLOYED | S3 adapter and local S3 emulator-style test PASS |
| Local CORS | PASS | exact origin allowed, random origin rejected |
| Production CORS | NOT VERIFIED | requires deployed frontend/backend origins |
| Production browser E2E | NOT VERIFIED | requires public URLs and demo credentials |
| Local browser E2E | PASS | 7/7 |
| Lighthouse | PASS (local) | 85/100/100/100 Compose; 99/100/100/100 Vite preview |
| Performance | PASS (local only) | p95 samples recorded above |
| SEO baseline | PASS | title/description/OG/JSON-LD/robots; crawlability remains partial for authenticated SPA |
| Secrets audit | PASS | Gitleaks history and working tree: no leaks |
| Rollback runbook | PASS | [`DEPLOYMENT_RUNBOOK.md`](../deployment/DEPLOYMENT_RUNBOOK.md) |

## Final status

**NOT RELEASE READY for public Internet deployment.** Provider-independent Phase 5 work is complete and locally verified. Real deployment is intentionally gated until the owner supplies cloud accounts/secrets and explicitly enables the production workflow variable. After that, rerun `verify-public.mjs`, deployed Playwright E2E, media persistence after redeploy, cold/warm performance and public CORS checks before calling the release ready.

No commit or push was performed. Recommended commit grouping: (1) backend production guard and media adapter, (2) Docker/Compose and frontend deployment config, (3) CI/deploy workflows, (4) ADR/runbook/report, (5) preserve the already pending Phase 4 V9 playback changes in their existing feature commit.
