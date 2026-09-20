# Phase 3.8 — Personalization & Discovery

Status: **implemented and runtime-verified locally**

## Delivered

- Added additive Flyway migration V6 for profile-scoped likes/dislikes and search history, including recent-search and signal indexes.
- Added preference APIs with idempotent LIKE/DISLIKE upsert and delete behavior.
- Added deterministic recommendations. Candidate movies exclude the active profile's watched and disliked titles, then score history affinity, rating, popularity and liked categories. Score ties resolve by movie id.
- Added personalized `/home` data for top picks, trending titles and continue-watching rows.
- Added profile-scoped recent searches with whitespace normalization, 120-character truncation and a 20-entry retention limit.
- Added public popular-search aggregation and authenticated title autocomplete.
- Added frontend Like/Dislike controls, personalized shelves, 300 ms autocomplete debounce, recent-search chips and popular-search chips.

## Chosen logic

Recommendation ranking uses a small deterministic batch of active candidates instead of a database-specific recommendation function: history/category affinity 45%, rating 25%, popularity 15% and liked-category affinity 15%. This keeps the result explainable and portable while the catalogue is still small; the candidate query is bounded to 100 rows and ordered by rating, views and id. The implementation also clamps popularity normalization to avoid a zero denominator when a catalogue has no views.

Search history is profile-scoped because profiles represent independent taste. Popular searches are intentionally aggregate across profiles for discovery. The frontend waits 300 ms and requires at least two characters before requesting suggestions, reducing request noise while keeping the input responsive.

## Evidence

```text
Backend startup after V6 and JPA mapping fix
Flyway: Successfully validated 6 migrations
Flyway: Current version of schema "public": 6
Flyway: Schema "public" is up to date. No migration necessary.
Hibernate: EntityManagerFactory initialized successfully
Spring Boot: Tomcat started on port 18080

mvn.cmd -q test
Process exit code: 0

mvn.cmd -q -DskipTests package
Process exit code: 0

npm.cmd run lint
tsc -b --pretty false
Process exit code: 0

npm.cmd run build
vite v6.4.3
✓ 171 modules transformed.
✓ built in 1.83s

Phase 3.8 API smoke
{"loginToken":true,"preference":"LIKE","recommendationCount":5,"deterministic":true,"homeTopPicks":12,"homeTrending":12,"recentCount":1,"popularCount":1,"suggestionCount":1,"invalidSignalStatus":400,"unauthStatus":403,"cleanup":"ok"}

Frontend route smoke via Vite 6.4.3
200 /
200 /browse
200 /search?q=inter
200 /movies/42
200 /account
200 /forgot-password
200 /reset-password?token=demo
200 /verify-email?token=demo
200 /admin
```

The Windows computer-use surface had no browser available, so visual browser inspection and console capture remain unverified. Backend runtime, database migration, API contract, TypeScript, production build and route checks passed.
