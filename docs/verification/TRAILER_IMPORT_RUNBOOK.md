# Legal trailer import runbook

This utility imports trailers only from an allow-list of official or clearly
authorized publisher pages. It stores a public YouTube watch URL (or the
explicitly approved Rubik Pictures MP4 for Bóng Đè); it never downloads or
hosts a full movie.

## Local prerequisites

- Java 21 available on `PATH`.
- Cinevora backend running on `http://localhost:8080`.
- PostgreSQL running through the existing Docker Compose setup.
- An admin account that can update movies.

Set credentials without printing the password:

```powershell
$env:CINEVORA_API_URL = 'http://localhost:8080'
$env:CINEVORA_ADMIN_USERNAME = 'admin'
$env:CINEVORA_ADMIN_PASSWORD = ((Get-Content .env | Select-String '^DEMO_PASSWORD=').Line -split '=', 2)[1].Trim()
```

## Compile and test

```powershell
Remove-Item -Recurse -Force tools/build/trailers -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force tools/build/trailers | Out-Null
javac -encoding UTF-8 -d tools/build/trailers `
  tools/WikimediaPosterImporter.java tools/YouTubeTrailerImporter.java tools/YouTubeTrailerImporterTest.java
java -cp tools/build/trailers YouTubeTrailerImporterTest
```

## Dry-run and apply

Dry-run is the default and performs no movie update requests:

```powershell
java -cp tools/build/trailers YouTubeTrailerImporter --dry-run
```

Apply preserves every existing movie field and changes only `videoUrl`:

```powershell
java -cp tools/build/trailers YouTubeTrailerImporter --apply
```

Existing non-empty `videoUrl` values are skipped by default. Use `--force` only
when intentionally replacing them. The generated evidence is written to
`docs/verification/TRAILER_IMPORT_REPORT.md`; apply mode reloads each updated
movie through the API and verifies the persisted URL.

The frontend recognizes YouTube watch, short, embed, and youtu.be URLs and
renders them through the privacy-enhanced `youtube-nocookie.com` iframe. Direct
MP4 sources continue to use the HTML5 video player.

Never put credentials in source or reports, and do not reset the database to
run this utility.
