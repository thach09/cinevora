# Wikimedia poster import runbook

This is a one-shot developer utility. Cinevora runtime does not call an
external movie provider. The utility reads movies through the Cinevora API,
queries the public Wikimedia Page Content Service / Wikipedia REST API, and
updates only `thumbnailUrl` through the existing admin movie endpoint. A small
allow-listed set of official rights-holder pages supplies posters when a
Wikimedia result is unavailable or ambiguous.

Wikimedia is used without an API key. Requests include a descriptive
`User-Agent`, throttle requests to respect provider rate limits, use bounded
retries with `Retry-After` handling, and treat HTTP 401, 403, 404, and other provider failures explicitly. Exact page title + release
year + film evidence + a valid portrait image are required for automatic
application. Search fallback results are `REVIEW_REQUIRED` and are never
applied automatically.

## Local prerequisites

- Java 21 available on `PATH`.
- Cinevora backend running on `http://localhost:8080`.
- PostgreSQL running through the existing Docker Compose setup.
- An admin account that can update movies.

Set the local API credentials in the PowerShell session without printing the
password:

```powershell
$env:CINEVORA_API_URL = 'http://localhost:8080'
$env:CINEVORA_ADMIN_USERNAME = 'admin'
$env:CINEVORA_ADMIN_PASSWORD = ((Get-Content .env | Select-String '^DEMO_PASSWORD=').Line -split '=', 2)[1].Trim()
```

## Compile and test the utility

```powershell
Remove-Item -Recurse -Force tools/build/wikimedia -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force tools/build/wikimedia | Out-Null
javac -encoding UTF-8 -d tools/build/wikimedia `
  tools/WikimediaPosterImporter.java tools/WikimediaPosterImporterTest.java
java -cp tools/build/wikimedia WikimediaPosterImporterTest
```

## Dry-run

The default is dry-run. It does not issue movie update requests.

```powershell
java -cp tools/build/wikimedia WikimediaPosterImporter --dry-run
```

Useful bounded checks:

```powershell
java -cp tools/build/wikimedia WikimediaPosterImporter --dry-run --limit=5
java -cp tools/build/wikimedia WikimediaPosterImporter --dry-run --movie-id=44
```

The generated evidence is written to
`docs/verification/POSTER_IMPORT_REPORT.md`. Check the `MATCHED_HIGH_CONFIDENCE`
rows and their poster URLs before applying.

## Apply and verify

```powershell
java -cp tools/build/wikimedia WikimediaPosterImporter --apply
```

Apply mode preserves the complete existing movie payload and changes only
`thumbnailUrl`. After each update, the utility reloads the catalog through the
admin API and marks the row `Verified=yes` only when the persisted URL equals
the requested URL.

Use `--force` only when intentionally replacing existing poster URLs:

```powershell
java -cp tools/build/wikimedia WikimediaPosterImporter --apply --force
```

Never put credentials in source, reports, or committed files. Do not reset the
database to run this utility.
