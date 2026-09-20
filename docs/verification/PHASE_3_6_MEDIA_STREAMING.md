# Phase 3.6 — Media & Streaming Core

Status: **implemented and runtime-verified locally**

## Delivered

- Added immutable Flyway migration `V3__media_and_playback.sql`.
- Added `MediaStorageService` abstraction with a local filesystem implementation.
- Added poster upload, replace and remove APIs under `/api/v1/media/**`.
- Validated poster size, JPG/JPEG/PNG/WEBP signatures, decoded dimensions for JPG/PNG, generated UUID filenames, and rejected non-image files.
- Kept `movies.thumbnail_url`; no binary data is stored in PostgreSQL.
- Exposed `/media/posters/**` as public read-only static media and kept upload/delete admin-only.
- Added real playback position and duration to `continue_watching`, with a user/update index and backfill from the existing percentage column.
- Centralized playback rules: under 2% is ignored, 2–89% is Continue Watching, and 90%+ becomes idempotent history.
- Replaced the manual percentage UI with an HTML5 player that supports browser controls, resume, seek, mute, speed selection, throttled progress writes, and force-save events.
- Added admin drag/drop poster preview, upload, replace and remove UI.

## Chosen logic

The implementation keeps the existing `percent` request/response field for compatibility, while treating `positionSeconds` and `durationSeconds` as the source of truth whenever available. Progress writes are throttled to about 7 seconds and are also attempted on pause, seek, ended, page hidden and page hide. The completion threshold is defined once in `WatchProgressPolicy` and covered by unit tests.

Filesystem storage is the smallest local implementation behind the required abstraction. A future S3/R2 adapter can replace it without changing controllers or the movie schema.

## Evidence

```text
Flyway: Successfully validated 3 migrations
Flyway: Migrating schema "public" to version "3 - media and playback"
Flyway: Successfully applied 1 migration ... now at version v3
Spring Boot: Tomcat started on port 18080

mvn.cmd -q test
Process exit code: 0

npm.cmd run lint
tsc -b --pretty false
Process exit code: 0

npm.cmd run build
vite v6.4.3
✓ 170 modules transformed.
✓ built in 8.66s

Phase 3.6 API smoke
health=UP
schema=v3
poster upload response contained /media/posters/...jpg
public poster HTTP=200
progress position=120, percent=20
completion history count=1
poster remove success=True
legacy low progress rows=0
invalid MIME upload HTTP=400
unauthenticated upload HTTP=403

Frontend runtime route smoke
/                 200
/login            200
/browse           200
/movies/41        200
/continue-watching 200
/admin/movies     200
```

The Windows computer-use surface reported no available browser (`apps=[]`, `browsers=[]`), so visual browser inspection and console capture could not be performed in this environment. Production build, Vite HTTP route smoke, API smoke and backend tests passed.
