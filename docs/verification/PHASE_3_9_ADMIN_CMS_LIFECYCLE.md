# Phase 3.9 — Admin CMS, Users & Lifecycle

Status: **implemented and runtime-verified locally**

## Delivered

- Added admin-only, paginated user search with `all`, `active` and `inactive` filters across username, email and full name.
- Added user activation/deactivation with current-admin self-lock protection and refresh-session revocation when an account is disabled.
- Added admin movie pagination that can include archived records, with status lifecycle endpoints.
- Added admin category lifecycle endpoints and a dedicated Archive & Restore workspace for archived movies and categories.
- Kept the existing movie form and poster upload CMS flow intact; archived recovery is now explicit and reversible.
- Added a dedicated Users workspace with search, status filter, pagination and actionable status controls.
- Added `X-Profile-Id` to the CORS allow-list so profile-scoped browser requests can complete preflight.
- No schema migration was needed: lifecycle uses the existing `is_active` columns and preserves related data.

## Chosen logic

Admin lifecycle endpoints are additive under `/api/v1/admin/...`, leaving public catalogue semantics unchanged. User deactivation revokes all refresh sessions while the JWT filter also checks `User.active`, so both access-token and refresh-token paths stop accepting a disabled account. The current admin cannot deactivate itself, preventing accidental lockout during CMS operations.

The archive workspace is separate from the active CRUD screens: this keeps the primary catalogue focused while making recovery discoverable and reversible. Movie search is paginated and deterministically ordered by the requested sort plus id; user search is similarly ordered by creation time plus id.

## Evidence

```text
Backend startup
Flyway: Successfully validated 6 migrations
Flyway: Current version of schema "public": 6
Flyway: Schema "public" is up to date. No migration necessary.
Hibernate: EntityManagerFactory initialized successfully
Spring Boot: Tomcat started on port 18080

mvn.cmd -q test
Process exit code: 0

mvn.cmd -q -DskipTests package
Process exit code: 0

Phase 3.9 admin API smoke
{"loginToken":true,"allUsers":11,"activeUsers":11,"inactiveUsers":0,"allMovies":64,"activeMovies":60,"categories":10,"movieArchiveRestore":true,"userLifecycle":true,"selfDeactivateStatus":400,"unauthStatus":403}

CORS preflight smoke
HTTP/1.1 200
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Headers: authorization, x-profile-id, content-type
Access-Control-Allow-Credentials: true

npm.cmd run lint
tsc -b --pretty false
Process exit code: 0

npm.cmd run build
vite v6.4.3
✓ 171 modules transformed.
✓ built in 1.93s

Frontend route smoke via Vite 6.4.3
200 /admin
200 /admin/movies
200 /admin/categories
200 /admin/users
200 /admin/archive
200 /admin/statistics
200 /browse
200 /search
```

The Windows computer-use surface had no browser available, so visual browser inspection and console capture remain unverified. API authorization, lifecycle behavior, CORS preflight, TypeScript, production build and route checks passed. When running Vite on `127.0.0.1` instead of `localhost`, configure `CORS_ALLOWED_ORIGINS` to include that origin.
