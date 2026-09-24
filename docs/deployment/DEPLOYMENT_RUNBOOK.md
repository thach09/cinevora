# Cinevora deployment runbook

This runbook deploys the exact tested commit. It does not reset databases, edit an applied migration, or use a developer database for verification.

## Local full-stack verification

```powershell
Copy-Item .env.example .env
docker compose --env-file compose.ci.env config --quiet
docker compose --env-file compose.ci.env up --build -d --wait --wait-timeout 180
docker compose --env-file compose.ci.env --profile tools run --rm flyway validate
```

The isolated profile uses `cinevora_ci_pgdata`, database `cinevora_ci`, backend port `18086` and frontend port `18088`. The normal developer volume is never reset by this runbook.

## Required production variables

Set these in the provider secret manager. Values below are names only; do not copy demo values into production.

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://...
DB_USERNAME=...
DB_PASSWORD=...
DB_SSL_MODE=require
JWT_SECRET=<random 48+ byte value>
CORS_ALLOWED_ORIGINS=https://<frontend-origin>
BOOTSTRAP_ADMIN_USERNAME=<unique-owner-username>
BOOTSTRAP_ADMIN_EMAIL=<unique-owner-email>
BOOTSTRAP_ADMIN_PASSWORD=<strong-one-time-secret>
MEDIA_STORAGE=s3
MEDIA_S3_ENDPOINT=https://<account>.r2.cloudflarestorage.com
MEDIA_S3_REGION=auto
MEDIA_S3_BUCKET=<bucket>
MEDIA_PUBLIC_BASE_URL=https://<public-media-domain>
MEDIA_S3_ACCESS_KEY=...
MEDIA_S3_SECRET_KEY=...
```

The guard refuses a dev profile, weak or known demo JWT secret, wildcard/local CORS, development token exposure, public Swagger, local media storage, missing object-storage settings, and verbose production logging. The first production boot also fails closed unless all three `BOOTSTRAP_ADMIN_*` variables are supplied. Set these manually in the provider secret manager, never in Git or a build argument. Choose a new username (8–50 letters/digits/underscores) and unique email; the password must be at least 20 characters, at most 72 UTF-8 bytes, contain at least 12 distinct characters, and must not contain “cinevora”. A missing, weak or duplicate identity aborts the transaction and startup.

The bootstrap runs once, disables the 11 seeded demo identities, revokes their sessions and records `security_bootstrap.completed_at`/`admin_user_id`. Verify the marker and zero active demo IDs through a restricted database/operator channel, then confirm the new ADMIN can log in and the demo ADMIN cannot. On later restarts the bootstrap is not repeated, so rotate the new admin password through the authenticated account flow and rotate/remove the one-time secret in the provider secret manager. For lost admin access, use a reviewed recovery procedure against a backup/disposable copy before applying any production data change; do not clear the marker or re-enable demo identities.

Vercel needs the build-time variable:

```text
VITE_API_URL=https://<backend-origin>/api/v1
```

The current `Secure; HttpOnly; SameSite=Lax` auth cookies **require a same-site frontend and API**. Use controlled HTTPS hostnames such as `https://cinevora.example` and `https://api.cinevora.example`, or an approved same-origin `/api` proxy. `FRONTEND_ORIGIN`, `BACKEND_ORIGIN`, `CORS_ALLOWED_ORIGINS` and built `VITE_API_URL` must describe that exact pair. Raw `*.vercel.app` plus `*.onrender.com`/`*.up.railway.app` is not a supported final production pair: a browser withholds the CSRF/refresh cookies. Do not relax SameSite, HttpOnly or token transport to compensate. No real domain has been selected, so public authentication remains unverified. The release gate must run the real-browser auth test against the selected hostnames before public release.

## Cloud creation order

1. Create PostgreSQL and record its private connection properties. Do not expose PostgreSQL publicly.
2. Attach the intended same-site API HTTPS hostname, create the Render Web Service from `render.yaml`, set all `sync: false` values including the three bootstrap secrets, and configure `/actuator/health` as the HTTP health check.
3. Wait for the backend health check and Flyway migration to complete.
4. Attach the matching frontend HTTPS hostname to the Vercel project rooted at `frontend/`, set `VITE_API_URL` to the API hostname, and deploy the tested commit.
5. Set `CORS_ALLOWED_ORIGINS` to the exact frontend HTTPS origin, then redeploy the backend.
6. Run public verification and the Playwright `public-auth-topology.spec.ts` browser gate using a disposable non-demo account. Verify login, cookie flags, refresh, reload, protected access and logout. Delete or archive test content afterward. A failed browser gate blocks release even if HTTP health and CORS pass.

The checked-in deploy workflow remains disabled until the repository variable `DEPLOY_ENABLED=true` is explicitly set. It requires `RENDER_API_KEY`, `RENDER_SERVICE_ID`, `VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`, `BACKEND_ORIGIN` and `FRONTEND_ORIGIN` in the production environment. Render deployment is pinned to the exact Git commit and health-checked before frontend deployment.

## Health and security checks

```powershell
node tools/deployment/verify-runtime.mjs
node tools/deployment/verify-runtime.mjs --after-restart
node tools/deployment/verify-public.mjs
```

Expected results include health 200 with status only, anonymous/protected endpoint rejection, CUSTOMER admin rejection, ADMIN access, foreign profile rejection, refresh-token rotation and revocation rejection, exact CORS allow/reject behavior, SPA deep links, poster upload authorization and media persistence after restart.

Do not print bearer tokens, refresh tokens, reset tokens, database passwords or object-storage credentials in logs.

## Rollback

- **Frontend:** use the previous successful Vercel deployment or redeploy the previous tested commit. Restore the previous backend origin only if that was part of the release.
- **Backend:** use Render rollback to one of the previous successful deploys, then run `/actuator/health` and the public smoke checks.
- **Database:** never delete Flyway history or edit an applied migration. If a rollback needs schema repair, deploy a new forward migration. Take a provider backup before a destructive data operation.
- **Media:** object keys are generated and immutable. Keep old objects until the database commit succeeds; replacement cleanup is best effort and must not delete an external URL.

## Diagnosis

| Symptom | Check |
|---|---|
| Backend fails before listening | Inspect the provider log for the configuration guard; verify prod profile, JWT, JDBC, CORS and R2 variables. |
| Flyway fails | Check database URL/SSL/user and migration history. Do not edit old SQL files. |
| Frontend API calls fail | Inspect the built `VITE_API_URL`, backend health, exact CORS origin and browser preflight response. |
| Posters disappear | Confirm `MEDIA_STORAGE=s3`, public URL, bucket permissions and that no production service is using `/app/uploads`. |
| SPA deep link is 404 | Confirm Vercel rewrites or the Nginx `try_files ... /index.html` rule. |
| Free Render service is slow | Account for cold start after idle; record warm and cold latency separately. |

## Generated files that stay local

`backend/target/`, `frontend/dist/`, Playwright traces/screenshots, Lighthouse JSON/HTML, `.tmp/`, local uploads, and the isolated Compose volumes are verification artifacts. They are ignored and should not be committed.
