# Cinevora - Phase 4 Final Integration Master

**Date:** 2026-09-21

**Scope:** Playwright browser integration, customer/admin/cross-role journeys, profile-cache isolation, console/network checks, responsive checks, Lighthouse and confirmed regression fixes.

**Result:** **PHASE 4 INTEGRATION PASS**

This is an integration PASS for the current local candidate. It is not a production-deployment approval; Phase 5 Docker/CI/storage/production-policy work remains separate.

## 1. Test environment

| Component                   | Observed value                                          |
| --------------------------- | ------------------------------------------------------- |
| Java                        | 21.0.12.1 LTS                                           |
| Spring Boot                 | 3.4.5                                                   |
| PostgreSQL                  | 16.15, isolated `cinevora_test` database                |
| Node/npm                    | Node 24.21.0 / npm 11.19.0                              |
| Playwright                  | 1.63.0                                                  |
| Lighthouse                  | 13.5.0                                                  |
| Browser                     | Installed Google Chrome via Playwright `executablePath` |
| Backend                     | `http://localhost:18080`, dev profile, `cinevora_test`  |
| Frontend dev                | `http://localhost:5173`                                 |
| Frontend production preview | `http://localhost:4173`                                 |

The developer database was not reset. No `docker compose down -v` was run. The dedicated test database was used for all mutations.

## 2. Setup and verification commands

The Playwright/Lighthouse setup is in [frontend/package.json](D:/GitHub/cinevora/frontend/package.json), [playwright.config.ts](D:/GitHub/cinevora/frontend/playwright.config.ts) and [phase4.spec.ts](D:/GitHub/cinevora/frontend/tests/e2e/phase4.spec.ts).

Key outputs:

```text
npm install --save-dev @playwright/test lighthouse
added 113 packages, and audited 287 packages
found 0 vulnerabilities

mvn.cmd clean test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

npm.cmd run build
vite v6.4.3
✓ 173 modules transformed.
✓ built in 1.45s

npm.cmd audit --audit-level=high
found 0 vulnerabilities

npm.cmd exec tsc -- --noEmit --pretty false
exit code 0
```

## 3. Playwright final result

Command:

```text
$env:CINEVORA_API_URL='http://localhost:18080/api/v1'; npm.cmd run e2e
```

Final raw result:

```text
Running 5 tests using 1 worker
ok 1 ... customer registration, browse, detail and personal library journey (3.4s)
ok 2 ... admin CMS journey and category business-rule rejection (1.2s)
ok 3 ... cross-role publish, customer discovery/play, and admin statistics (10.4s)
ok 4 ... profile switching never shows another profile’s watchlist cache (1.1s)
ok 5 ... console/network health and responsive layouts at 375/768/1280/1440 (3.0s)

5 passed (22.3s)
```

### Customer E2E - PASS

- Register a unique customer through the UI.
- Redirect to Browse and load the catalogue.
- Open a movie detail page.
- Add to watchlist and favourites.
- Mark as watched.
- Verify the movie appears in watchlist, favourites and history.

### Admin E2E - PASS

- Login as admin.
- Create a unique category through the CMS.
- Create a unique movie with a video source through the CMS.
- Attempt to archive the category while an active movie references it.
- Verify the UI preserves the category and the API returns HTTP 400.

### Cross-role E2E - PASS

- Admin publishes a category and movie.
- Customer searches for and opens the newly published movie.
- Customer opens the player and marks the movie watched.
- Admin statistics page remains available.
- Exact backend statistics confirm the total view count increased by at least one.

### Profile switching cache isolation - PASS

- Create a second customer profile.
- Add `Avengers: Endgame` to that profile's watchlist.
- Switch back to the default profile without reloading the browser context.
- Verify the second profile's movie is not displayed.

### Console and network health - PASS

The final suite observed no unexpected browser console errors, page errors, failed requests or HTTP 5xx responses. The single expected HTTP 400 from the category business-rule test was explicitly treated as the intentional validation result, not an application failure.

### Responsive checks - PASS

The suite exercised 375, 768, 1280 and 1440 pixel viewports. It verified the main content remained visible, no horizontal overflow occurred, and the mobile menu opened/closed at the narrow viewport. Screenshots were generated under the ignored Playwright test-results directory.

## 4. Lighthouse final result

Lighthouse was run against the production Vite preview, not the development server:

```text
npm.cmd run build
npm.cmd run preview -- --host localhost --port 4173
npm.cmd run lighthouse
```

Final scores from `production-login`:

| Category       | Score |
| -------------- | ----: |
| Performance    |    99 |
| Accessibility  |   100 |
| Best Practices |   100 |
| SEO            |   100 |

Representative metrics:

```text
FCP 1.5 s
LCP 2.0 s
TBT 0 ms
CLS 0
```

Artifacts are generated under the ignored `frontend/lighthouse-report/` directory. The Lighthouse npm script now targets `http://localhost:4173/login`.

## 5. Confirmed regressions fixed

### Registration did not create a default profile

Browser evidence: a newly registered customer reached Browse, but profile-scoped watchlist actions returned `Không tìm thấy profile mặc định`.

Fix: [AuthService.java](D:/GitHub/cinevora/backend/src/main/java/com/cinevora/service/AuthService.java) now creates a default `Profile` for a newly registered user inside the registration transaction.

Verification: customer registration and personal-library E2E passed after the rebuilt backend was restarted.

### Profile-scoped queries reused another profile's cache

Browser/static evidence: query keys such as watchlist, favourites, history, continue-watching, preferences, search history and home omitted `activeProfileId`.

Fixes in [BrowsePages.tsx](D:/GitHub/cinevora/frontend/src/pages/BrowsePages.tsx), [LibraryPages.tsx](D:/GitHub/cinevora/frontend/src/pages/LibraryPages.tsx) and [MovieDetailPage.tsx](D:/GitHub/cinevora/frontend/src/pages/MovieDetailPage.tsx): profile-sensitive keys include `activeProfileId`, and dependent queries wait until a profile is selected.

Verification: profile switching E2E passed without a full reload.

### Admin movie form silently rejected an empty optional duration

Browser evidence: leaving Duration blank caused Zod coercion to turn the empty string into `0`; the form did not submit a movie POST.

Fix: [AdminPages.tsx](D:/GitHub/cinevora/frontend/src/pages/AdminPages.tsx) uses a preprocessing schema for optional positive numeric values.

Verification: admin and cross-role movie creation E2E passed.

### Admin movie form had no video source field

Fix: the CMS now exposes the existing backend `videoUrl` field as an optional Video source URL input. No schema or API response shape changed.

Verification: cross-role E2E created a movie, opened the player and completed the watched flow.

### Lighthouse accessibility landmark and browser 404 noise

Fixes:

- Auth pages now use a semantic `<main>` landmark in [AuthPages.tsx](D:/GitHub/cinevora/frontend/src/pages/AuthPages.tsx).
- Added [favicon.svg](D:/GitHub/cinevora/frontend/public/favicon.svg) and linked it from [index.html](D:/GitHub/cinevora/frontend/index.html).

Verification: production-preview Lighthouse returned Accessibility 100 and SEO 100; the final browser suite reported no unexpected console/network errors.

## 6. Existing Phase 1-3 integration baseline retained

The prior pre-Phase-4 audit remains at [PHASE_4_PRE_DEPLOYMENT_MASTER_AUDIT.md](D:/GitHub/cinevora/docs/verification/PHASE_4_PRE_DEPLOYMENT_MASTER_AUDIT.md). Its verified baseline remains applicable:

- PostgreSQL/Flyway V1-V8 validation passed on developer and isolated test databases.
- Backend API/security smoke and CORS checks passed.
- Archived-movie public media-track exposure was fixed and regression-tested.
- User-library N+1 query behavior was fixed with join-fetch repository reads.

## 7. Final Phase 4 gate

| Gate                          | Result | Evidence                                                         |
| ----------------------------- | ------ | ---------------------------------------------------------------- |
| Playwright setup              | PASS   | Playwright 1.63.0, 5 tests discovered and executed               |
| Customer E2E                  | PASS   | Registration, browse, detail, watchlist, favourite, history      |
| Admin E2E                     | PASS   | CMS create flows and category rule HTTP 400                      |
| Cross-role E2E                | PASS   | Admin publish -> customer discovery/play -> statistics increment |
| Profile switching cache       | PASS   | Cross-profile watchlist isolation without reload                 |
| Console/network errors        | PASS   | No unexpected console, page, failed-request or 5xx evidence      |
| Responsive 375/768/1280/1440  | PASS   | No horizontal overflow; mobile menu verified                     |
| Lighthouse production preview | PASS   | 99 / 100 / 100 / 100                                             |
| Backend tests                 | PASS   | 6/6 tests, zero failures/errors                                  |
| Frontend build/type/audit     | PASS   | Build, TypeScript and 0 vulnerabilities                          |

**Final status: PHASE 4 FINAL INTEGRATION PASS.**

This PASS does not close Phase 5 deployment prerequisites: backend/frontend containerization, CI workflows, persistent production media storage, production secret/profile enforcement, Swagger exposure policy and production-like load testing remain outstanding.

No commit or push was performed.
