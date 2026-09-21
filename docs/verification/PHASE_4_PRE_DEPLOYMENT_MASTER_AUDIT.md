# Cinevora — Phase 4 Pre-Deployment Master Audit

**Audit date:** 2026-09-21

**Commit tested:** 35f129f — docs: update master report for phase 3.11 verification

**Scope:** independent pre-Phase-4/pre-deployment verification of the integrated backend, frontend, PostgreSQL/Flyway and security boundaries. No commit or push was performed.

## 1. Environment

| Component | Observed environment |
|---|---|
| OS | Windows PowerShell |
| Java | 21.0.12.1 LTS |
| Maven | 3.9.16 |
| Spring Boot | 3.4.5 |
| PostgreSQL | 16.15 in Docker Desktop |
| Flyway CLI | 10.22.0; schema migrations through V8 |
| Node/npm | Node 24.21.0, npm 11.19.0 |
| Frontend | React 18, Vite 6.4.3, TypeScript, React Router 7.18.4 |
| Local backend | http://localhost:18080 for dev-profile integration; 18081 for prod-profile startup check |
| Local frontend | http://localhost:5173 |

The repository was clean at the start of the audit. The final working tree contains only the localized fixes and the new audit report; no unrelated changes were reset or removed.

## 2. Commands actually executed

Key commands and observed results:

~~~text
git status --short
# clean at audit start

git log --oneline -10
35f129f docs: update master report for phase 3.11 verification

docker compose up -d postgres
Container cinevora-postgres Running

docker compose ps
cinevora-postgres postgres:16-alpine Up ... (healthy) 0.0.0.0:5432->5432/tcp

docker compose --profile tools run --rm flyway validate
Successfully validated 8 migrations

docker compose --profile tools run --rm -e FLYWAY_URL=jdbc:postgresql://postgres:5432/cinevora_test flyway migrate
Successfully applied 8 migrations to schema "public", now at version v8

mvn.cmd clean test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

mvn.cmd package
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

npm.cmd audit --audit-level=high
found 0 vulnerabilities

npm.cmd exec tsc -- --noEmit --pretty false
exit code 0

npm.cmd run build
vite v6.4.3
✓ 173 modules transformed.
✓ built in 1.48s

git diff --check
# no whitespace errors; only Windows LF/CRLF warnings
~~~

The first non-elevated Maven/npm attempts were blocked by sandbox network/filesystem permissions, not project failures. The same commands were rerun with the required access and completed successfully.

## 3. Database and Flyway status

The developer database was not destroyed and docker compose down -v was not run.

A separate cinevora_test database was created for deterministic integration testing. It started empty and migrated V1 through V8 successfully. The runtime backend was then started against that database.

Final checks:

~~~text
developer database: PostgreSQL 16.15, Flyway version 8
test database: PostgreSQL 16.15, Flyway version 8
developer Flyway validate: Successfully validated 8 migrations
test Flyway validate: Successfully validated 8 migrations
developer movie/category orphans: 0
test movie/category orphans: 0
test profile/watchlist orphans: 0
developer BCrypt seed rows: 11
test BCrypt rows: 12 (11 seed users plus the isolated QA registration)
~~~

The test database contained the expected seed baseline (11 users, 7 categories, 60 seed movies, profile-scoped seed data) before the isolated API mutations. Existing V1–V8 migration files were not edited.

## 4. Backend build and runtime

mvn.cmd clean test and mvn.cmd package both passed. The final suite contains 6 tests across 4 test classes:

- authentication invalid-credential behavior;
- category lifecycle protection;
- watch-progress policy boundaries;
- archived-movie media-track visibility regression.

Runtime evidence:

~~~text
GET http://localhost:18080/actuator/health -> 200 {"status":"UP"}
Spring Boot started on port 18080
Flyway validated 8 migrations; schema public is at version 8
Hibernate EntityManagerFactory initialized successfully

prod-profile startup with explicit DATABASE_URL/DB_USERNAME/DB_PASSWORD/
JWT_SECRET/CORS_ALLOWED_ORIGINS:
profile=prod, health=UP, swagger=200, apiDocs=200
~~~

The test suite emits the known Mockito/Byte Buddy self-attaching-agent warning on JDK 21. It does not fail the build, but should be addressed before a future JDK disallows dynamic agent loading.

## 5. Frontend build and runtime

Final frontend checks passed:

~~~text
npm audit: found 0 vulnerabilities
TypeScript no-emit check: exit code 0
Vite build: 173 modules transformed
entry JS: 300.25 kB, gzip 99.01 kB
CSS: 59.90 kB, gzip 8.94 kB
~~~

Vite runtime route smoke returned HTTP 200 and text/html for '/', '/login', '/browse', '/search?q=QA', '/movies/61', '/continue-watching', '/account', '/admin', '/admin/movies' and '/admin/media'. robots.txt returned HTTP 200, text/plain, 150 bytes.

This is static/HTTP runtime evidence only. It is not a browser click-through result.

## 6. API integration matrix

The broad API smoke executed 91 expected-status checks against the real Spring Boot process and PostgreSQL test database. The final critical regression run covered auth, browse, detail, search, suggestions, trending, home, recommendations, ownership, watchlist, favourites, progress, completion/history, preferences, notifications, admin pagination/statistics, CORS and the archived-track regression. The corrected search-history call returned save=200, entries=1, clear=200.

| Area | Runtime evidence | Result |
|---|---|---|
| Authentication | Admin/customer login 200; invalid and nonexistent account 400; registration 201; reset/verify flow 200 | PASS |
| Refresh/session | Rotation 200; logout followed by refresh rejection 400 | PASS |
| Protected access | No-token and malformed-token calls returned 403 | PASS |
| Profile ownership | Foreign profile header returned 404; own profile create/select/delete succeeded | PASS |
| Movies | Paging, sorting, filtering, search, suggestions, trending and detail returned 200 | PASS |
| Categories | Customer mutation 403; admin create/update/archive/restore succeeded | PASS |
| Lifecycle | Active movie blocked category archive with 400; archived movie detail returned 404; restore returned 200 | PASS |
| User data | Watchlist/favourite idempotency, progress, completion-to-history, CSV export and cleanup paths exercised | PASS |
| Discovery | Preference LIKE/upsert/delete, search history, recommendations and personalized home exercised | PASS |
| Notifications | Inbox and mark-all-read exercised; ownership uses authenticated user | PASS |
| Admin | Users, movies, categories, tracks and statistics returned 200; customer statistics/admin tracks returned 403 | PASS |
| Response contract | Successful JSON endpoints returned ApiResponse.success=true; expected errors returned success=false; pagination content/metadata observed | PASS |
| CSV export | history/export returned 200 CSV rather than JSON, as required for a download endpoint | PASS |

The initial broad harness had two reporting-script mistakes (PowerShell $HOME name collision and one omitted auth header), not API failures. The affected search-history call was rerun correctly and passed.

## 7. Browser E2E matrix

Computer-use inventory returned:

~~~text
apps=[]
browsers=[]
~~~

Therefore real-browser actions, console capture, network waterfall capture, screenshots and responsive interaction were NOT VERIFIED.

| Journey | Status |
|---|---|
| Customer register/login → browse → search → detail → play → progress → continue watching | NOT VERIFIED in browser; API and route smoke only |
| Customer watchlist/favourite/preference/history/recommendations/notifications/account/logout | NOT VERIFIED in browser; API paths exercised |
| Admin login → dashboard → users → CMS → tracks → publish/archive/restore/statistics | NOT VERIFIED in browser; admin API paths exercised |
| Cross-role admin publish → customer discovery → activity → statistics | NOT VERIFIED in browser; API lifecycle and statistics paths exercised |
| Chrome console/React warnings/failed requests | NOT VERIFIED |
| Responsive 375/768/1280/1440 px | NOT VERIFIED |
| Lighthouse | NOT VERIFIED; browser surface unavailable |

## 8. Security regression

Observed runtime results:

~~~text
anonymous protected endpoint: 403
malformed JWT protected endpoint: 403
CUSTOMER -> ADMIN statistics: 403
CUSTOMER -> admin track list: 403
foreign X-Profile-Id: 404
invalid X-Profile-Id: 404
valid localhost CORS preflight: 200 with Allow-Credentials=true
disallowed origin preflight: 403
disabled QA account login: 400
~~~

No tracked .env file was found. .env is ignored; only .env.example files are tracked. No production private key or raw password was found in source/config search. Seed passwords are BCrypt hashes in the database and the demo credential is explicitly documented as non-production.

Security/deployment risks still requiring Phase 5 treatment:

1. application.yml has a known dev JWT fallback and spring.profiles.default: dev; production must explicitly set SPRING_PROFILES_ACTIVE=prod and a strong JWT_SECRET.
2. Development configuration exposes development email/reset tokens. This must never be active in production.
3. Swagger and OpenAPI were reachable in the prod-profile smoke (200). A deliberate production exposure decision or restriction is still required.
4. Debug-level runtime logging used for the N+1 investigation rendered authentication response objects in request logs. Production logging is configured at INFO, but token-bearing response logging should be verified as absent in the final deployment image.

## 9. SEO audit

Static output checks on frontend/index.html and public/robots.txt:

~~~text
title: present
description: present
Open Graph baseline: present
robots meta: present (index,follow)
canonical in initial HTML: absent; injected client-side by Seo.tsx
JSON-LD in initial HTML: absent; injected client-side on MovieDetailPage
sitemap.xml: absent
robots.txt: present
~~~

Technical SEO baseline is present: dynamic title/description, canonical, Open Graph and Movie JSON-LD are implemented in React, and private paths are disallowed in robots.txt.

Full crawlability is incomplete. The catalogue and movie routes are behind the authenticated SPA route tree, and the canonical/JSON-LD metadata is client-injected. A crawler cannot reliably discover useful public movie pages from the initial HTML. This is therefore:

- Technical SEO baseline: PASS
- Crawlability/discoverability: PARTIAL

No SSR, prerendering, public movie route or sitemap was introduced during this audit. Those are future architecture proposals, not silent changes.

## 10. Performance measurements

Measurements were taken on the local Windows development machine after warm-up, with 20 sequential HTTP requests per endpoint against PostgreSQL 16.15. These are engineering regression checks, not production SLAs.

| Endpoint | p50 | p95 | Max | Status |
|---|---:|---:|---:|---|
| Movies list | 22.84 ms | 24.93 ms | 25.78 ms | 200 |
| Search | 19.09 ms | 21.23 ms | 21.65 ms | 200 |
| Recommendations | 27.78 ms | 30.62 ms | 32.19 ms | 200 |
| Personalized home | 37.07 ms | 40.32 ms | 40.57 ms | 200 |
| Watchlist | 20.27 ms | 29.37 ms | 31.60 ms | 200 |
| History | 20.53 ms | 28.14 ms | 30.07 ms | 200 |
| Continue watching | 20.03 ms | 26.16 ms | 26.49 ms | 200 |
| Notifications | 19.45 ms | 25.89 ms | 26.99 ms | 200 |

All measured p95 values were below the development targets of approximately 300 ms for common GET, 400 ms for search and 500 ms for recommendations/home.

### N+1 finding and fix

Before the fix, Hibernate SQL for a two-row watchlist showed one relation query followed by one movie SELECT per row. The watchlist, favourites, history and continue-watching repository reads now use JOIN FETCH for their movie relation. After the fix, the watchlist request emitted one watchlist JOIN movies query for the collection.

The change preserves response shapes and was verified by the final 6-test clean suite plus runtime watchlist/history/continue-watching calls.

Frontend static review confirmed:

- route-level React.lazy code splitting;
- 300 ms autocomplete debounce and minimum two-character threshold;
- progress writes throttled to approximately 7 seconds with lifecycle flush attempts;
- TanStack Query stale time on suggestions/tracks/notifications.

The actual browser network waterfall and rerender profile remain NOT VERIFIED.

## 11. Database/query findings

- Flyway V8 indexes are applied and validated on both developer and test databases.
- Public movie/search/trending/recommendation queries use bounded pagination/candidate sets.
- Recommendation candidate selection is bulk-based and does not perform a query per movie.
- User-library N+1 behavior was found and fixed with targeted join-fetch queries.
- No unbounded movie/user admin list endpoint was observed; admin lists use page/size constraints.
- No schema migration was added; V1–V8 remain immutable.

## 12. Frontend/network findings

Static evidence shows parallel query declarations on major screens, route lazy loading, debounced suggestions and throttled playback persistence. HTTP smoke proved SPA fallback and the expected localhost:5173 origin.

Remaining frontend integration risk: query keys such as watchlist, history and home do not include the active profile id, and setActiveProfile does not globally invalidate the query cache. The backend ownership boundary is correct, but a browser E2E test is still needed to prove that switching profiles cannot briefly display stale cached data.

## 13. Deployment-readiness findings

Production-profile configuration was startup-tested with explicit environment variables and Hikari settings. However, the repository is not yet deployment-ready:

- no backend Dockerfile;
- no frontend Dockerfile or Nginx SPA configuration;
- no .github/workflows directory or CI workflow;
- root Compose currently manages PostgreSQL/Flyway only, as anticipated by the Phase 5 roadmap;
- no production object/media storage adapter or persistent filesystem strategy;
- Swagger production exposure is unresolved;
- profile/secret guardrails are not enforced by the repository;
- no Lighthouse or concurrency/load-test evidence.

## 14. Automated test gap analysis

| Feature | Existing automated coverage | Manual/runtime coverage | Missing coverage | Priority |
|---|---|---|---|---|
| Authentication/login | 1 unit test | API login/invalid/disabled/registration/reset/verify | Spring integration tests for all auth/session paths | High |
| Refresh/session | None dedicated | Rotation, revoke, rejected refresh | Integration tests for rotation/revocation/expiry | High |
| Profile ownership | None dedicated | Foreign header and isolated profile API checks | Automated ownership matrix | High |
| Role authorization | None dedicated | CUSTOMER admin calls returned 403 | Controller/security integration tests | High |
| Movie/category lifecycle | Category unit test | Create/update/archive/restore and rule rejection | Full DB-backed lifecycle tests | High |
| Watch progress/history | 3 policy unit tests | Progress, completion, history/export | Repository/service integration tests | High |
| Recommendations/search | None | API smoke and timing spot checks | Determinism, exclusion and query-shape tests | Medium |
| Tracks/media | New archived-track unit regression | Track CRUD/archive and visibility API checks | Multipart poster/video integration tests | High |
| Notifications | None | Inbox/read/read-all API checks | Ownership/read-state integration tests | Medium |
| Frontend components | None | HTTP route/build/static review | Component tests and error/loading-state tests | Medium |
| Browser E2E | None | NOT VERIFIED; no browser surface | Playwright or smallest Phase 4 E2E setup | Blocker before release |
| CI/reproducibility | No workflow | Local clean build only | GitHub Actions for backend/frontend/test DB | High |

The current 6-test count is not a quality score; the main gap is the absence of DB-backed integration tests and real-browser E2E.

## 15. Bugs discovered

### HIGH — archived movie exposed active media tracks

Evidence before fix:

~~~text
PATCH /api/v1/admin/movies/61/status {"active":false} -> 200
GET   /api/v1/movies/61                    -> 404
GET   /api/v1/movies/61/tracks              -> 200
ARCHIVED_TRACK_COUNT=1
~~~

This allowed public track metadata/source URLs to remain visible for archived content.

### MEDIUM — N+1 query pattern in user libraries

Evidence before fix: a two-row watchlist produced the collection query plus two individual movie selects. This was measured with Hibernate SQL logging.

### LOW — stale repository documentation

The root README still opens as a Java CLI-only application and contains extensive CLI architecture/run instructions, while the current repository is a Spring Boot + React full-stack application. Phase 4 did not rewrite it because this is documentation scope, not a runtime defect.

## 16. Bugs fixed and verified

1. MediaTrackService.publicList now requires an active movie before returning public tracks. The new MediaTrackServiceTest asserts archived movies throw ResourceNotFoundException without querying tracks. Runtime regression after the fix:

   ~~~text
   archiveStatus=200, archivedTrackStatus=404, restoreStatus=200
   ~~~

2. Watchlist, favourites, history and continue-watching reads now join-fetch their movie relation. Hibernate SQL confirmed one joined collection query for watchlist, and the p95 spot checks remained below budget.

No schema, migration, authentication architecture or public response shape was changed.

## 17. Remaining risks

- Browser E2E, console inspection, responsive checks and Lighthouse are unavailable in this environment.
- Profile-switch cache invalidation is not browser-verified and may briefly show stale profile data.
- Production must explicitly select the prod profile and provide non-default secrets.
- Swagger/API docs exposure in prod requires an intentional decision.
- No real multipart poster upload or browser video playback was observed in a browser.
- No concurrent load test or production-like network test was run.
- Mockito dynamic-agent warning should be resolved before future JDK changes.

## 18. Known limitations

The application remains an authenticated React SPA. Technical metadata exists, but public search-engine discovery of movie pages is incomplete. This audit did not introduce SSR, prerendering, a public catalogue route or a sitemap.

The test database is intentionally retained for repeatability during this session and is separate from cinevora_db. No developer volume was reset.

## 19. Phase 5 prerequisites

Before deployment work is considered complete:

1. Add backend/frontend Dockerfiles, Nginx SPA fallback and full-stack Compose health checks.
2. Add GitHub Actions for clean backend tests, frontend type-check/build and deterministic PostgreSQL migration tests.
3. Enforce SPRING_PROFILES_ACTIVE=prod; remove/guard dev token exposure and known JWT fallbacks in deployment.
4. Decide and implement production Swagger/API-doc exposure policy.
5. Define persistent object/media storage and upload validation in the target platform.
6. Add browser E2E coverage for customer, admin and cross-role journeys.
7. Run Lighthouse and a concurrency/load spot test on the deployment candidate.
8. Decide whether public movie discovery is required; if yes, propose public read-only routes plus sitemap/prerender/SSR with API/security/deployment impact before implementation.
9. Resolve or formally accept the profile-query-cache invalidation risk with an automated browser test.

## 20. Final status

| Gate | Status | Evidence |
|---|---|---|
| Database | PASS | Docker healthy; integrity checks zero orphans; dedicated test DB migrated from empty |
| Flyway | PASS | Both databases validated all 8 migrations at V8 |
| Backend Tests | PASS | mvn.cmd clean test: 6/6, 0 failures/errors |
| Backend Runtime | PASS | Dev and explicit prod profile health UP; Hibernate/Flyway initialized |
| API Integration | PASS | Broad smoke plus final critical API/security/lifecycle checks; archived-track regression fixed |
| Frontend Build | PASS | npm audit 0 vulnerabilities; TypeScript and Vite build pass |
| Browser E2E | NOT VERIFIED | No browser surface available (apps=[], browsers=[]) |
| Security | PASS with deployment caveats | Authorization/ownership/CORS runtime checks pass; prod secret/Swagger policy remains |
| Performance | PARTIAL | Backend local p95 spot checks pass; browser waterfall and concurrency not verified |
| SEO Baseline | PASS | Title/description/OG/client SEO/robots baseline present |
| SEO Crawlability | PARTIAL | Authenticated SPA and client-injected metadata limit crawler discovery; no sitemap |
| Deploy Readiness | NOT READY | Phase 5 Docker/CI/storage/production-policy prerequisites remain |

**Overall gate:** suitable to enter Phase 4 integration work with the documented fixes, but **not cleared for production deployment** until browser E2E, production guardrails and Phase 5 prerequisites are addressed.

