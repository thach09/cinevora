# Playback source separation verification

## Scope

The catalogue now has two explicit media contracts:

- `videoUrl`: Watch Now source. Direct playback is the only path that saves Continue Watching and can complete into history.
- `trailerUrl`: promotional trailer source. Trailer playback never writes Continue Watching, progress, or history.

V1–V8 were not edited. V9 adds `movies.trailer_url` and moves only unambiguous existing trailer URLs.

## Data audit and migration

Before V9, the active catalogue had 60 movies with 60 legacy `videoUrl` values: 59 YouTube URLs and one official Rubik Pictures MP4 trailer. The inactive catalogue added three duplicate trailer rows. No Watch Now source was present and no row required manual review.

Runtime Flyway log:

```text
Successfully validated 9 migrations
Current version of schema "public": 9
Schema "public" is up to date. No migration necessary.
```

Runtime API audit after migration:

```text
INCLUDE_INACTIVE=False TOTAL=60 WITH_TRAILER=60 WITH_VIDEO=0 YOUTUBE_TRAILER=59
INCLUDE_INACTIVE=True TOTAL=67 WITH_TRAILER=63 WITH_VIDEO=0 YOUTUBE_TRAILER=62
SAMPLE_FIELDS videoUrl=False trailerUrl=True
```

## Verification matrix

| Scenario | Expected | Result |
|---|---|---|
| Trailer YouTube iframe | Render privacy-enhanced embed | PASS |
| Trailer → History | No write | PASS |
| Trailer → Continue Watching | No write | PASS |
| Watch Now direct source | Render HTML5 player and use WATCH mode | PASS |
| Watch Now progress policy | Existing `WatchProgressPolicy` thresholds remain active | PASS |
| Watch Now completion | Existing service writes history at completion | PASS |

## Commands and evidence

```powershell
cd backend
mvn.cmd -q test
# exit 0

mvn.cmd -q package -DskipTests
# exit 0

cd ..\frontend
npm.cmd exec -- tsc -p tsconfig.app.json --noEmit --incremental false
npm.cmd exec -- tsc -p tsconfig.node.json --noEmit --incremental false
# both exit 0

$env:CINEVORA_API_URL='http://localhost:8080/api/v1'
$env:CINEVORA_FRONTEND_URL='http://localhost:5173'
npm.cmd run e2e -- --grep "trailer playback is separate" --output=..\playback-separation-e2e-20260922 --reporter=line
# 1 passed

npm.cmd run e2e -- --output=..\cinevora-e2e-final-20260922 --reporter=line
# 6 passed (17.4s)
```

Standalone importer assertions:

```text
WikimediaPosterImporterTest: 24 assertions passed
YouTubeTrailerImporterTest: 13 assertions passed
```

The full Phase 4 suite passes after using isolated Playwright output directories and keeping API fixture setup separate from the browser journey:

```text
6 passed (17.4s)
```
