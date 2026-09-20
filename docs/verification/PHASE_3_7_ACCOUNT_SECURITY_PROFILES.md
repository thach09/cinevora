# Phase 3.7 — Account, Security & Profiles

Status: **implemented and runtime-verified locally**

## Delivered

- Added additive Flyway migrations V4 and V5; V1–V3 remain immutable.
- Added account read/update APIs and current-password verification for password changes.
- Added opaque refresh sessions with SHA-256 token hashes, expiry, rotation and revocation.
- Added Axios single-flight refresh locking so concurrent 401 responses share one refresh request.
- Added logout/revoke, active-session listing and per-session revoke APIs.
- Added forgot/reset-password token lifecycle with expiration, one-time use and session revocation.
- Added email verification token lifecycle. Development-only token exposure is enabled only in `application-dev.yml`; the default/prod value is false.
- Added bounded in-memory rate limiting for login and reset actions.
- Added `profiles` with a default profile, a maximum of five profiles, ownership validation and CRUD/select APIs.
- Migrated watchlist, favourites, history and continue-watching ownership to `profile_id`; existing data was backfilled to each user’s default profile.
- Added profile selector in the authenticated layout and an Account page for profile, password and session management.

## Chosen logic

Refresh tokens are opaque random values and only their SHA-256 hashes are persisted. Access JWTs remain compatible through the existing `token` field, but the development default lifetime is now 15 minutes and refresh sessions default to 30 days. The browser never sends an unvalidated profile ownership decision: `X-Profile-Id` is checked against the authenticated username before every personal-data query.

Local profile data uses an additive header selector rather than embedding a mutable profile choice into the account table. This keeps account-level security separate from profile-scoped media libraries and lets the frontend switch profiles without re-authenticating.

## Evidence

```text
Flyway: Successfully validated 5 migrations
Flyway: Migrating schema "public" to version "4 - account sessions and security"
Flyway: Migrating schema "public" to version "5 - profiles and personal data ownership"
Flyway: Successfully applied 2 migrations ... now at version v5
Hibernate: EntityManagerFactory initialized successfully
Spring Boot: Tomcat started on port 18080

mvn.cmd -q test
Process exit code: 0

npm.cmd run lint
tsc -b --pretty false
Process exit code: 0

npm.cmd run build
vite v6.4.3
✓ 171 modules transformed.
✓ built in 1.76s

Phase 3.7 API smoke
schema=v5
login refreshToken=True
account username=admin
profileCount=1, defaultProfile=True
sessionCount=1
refresh access token=True
profile select=200
profile-scoped watchlist rows=1
default-profile watchlist rows=0
profile delete=200
forgot development token=True (dev profile only)
reset success=True
login after reset=True

Frontend route smoke
200 /account
200 /forgot-password
200 /reset-password?token=demo
200 /verify-email?token=demo
200 /browse
200 /admin
```

The Windows computer-use surface again had no browser available, so visual browser inspection and console capture remain unverified. Build, route, API and backend runtime checks passed.
