# Wikimedia Poster Import Report

- Execution date: 2026-09-21T10:51:02.5881826Z
- Cinevora commit: `2019f31`
- Provider: Wikimedia Page Content Service / Wikipedia REST API
- Mode: `DRY-RUN`
- Force overwrite: `false`
- Cinevora API: `http://localhost:8080/api/v1`

## Summary

| Metric | Count |
|---|---:|
| TOTAL | 1 |
| ALREADY_HAS_POSTER | 0 |
| MATCHED_HIGH_CONFIDENCE | 0 |
| REVIEW_REQUIRED | 1 |
| AMBIGUOUS | 0 |
| NOT_FOUND | 0 |
| NO_POSTER | 0 |
| ERROR | 0 |

## Match decisions

| Cinevora ID | Cinevora title/year | Wikimedia title | Page language | Poster | Decision | Applied | Verified | Page | Poster URL | Reason |
|---:|---|---|---|---|---|---|---|---|---|---|
|41|Avatar / 2009|Avatar|en|yes|REVIEW_REQUIRED|no|-|https://en.wikipedia.org/wiki/Avatar|https://thumb.wikimedia.org/wikipedia/commons/thumb/5/57/Vishnu_Avatars.jpg/60px-Vishnu_Avatars.jpg?utm_source=en.wikipedia.org&utm_campaign=rest&utm_content=thumbnail|SEARCH_FALLBACK_NEEDS_REVIEW|

## Apply verification

Dry-run completed; no Cinevora movie update request was issued.

No schema, videoUrl, frontend API client, or runtime external-provider dependency was changed.
