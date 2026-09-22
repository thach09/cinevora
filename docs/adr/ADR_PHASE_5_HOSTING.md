# ADR Phase 5 — Hosting architecture

**Date:** 2026-09-22
**Status:** Accepted for deployment preparation; cloud deployment pending account credentials

## Decision

Cinevora uses this logical topology:

```text
Browser → Vercel Hobby static SPA → Render Web Service (Spring Boot)
                                      ↓ TLS
                               Managed PostgreSQL

Poster upload → Cloudflare R2 / S3-compatible object storage
```

The repository keeps a reproducible Docker image for the frontend even though the cloud frontend is deployed as a Vite static site. Render is represented by [`render.yaml`](../../render.yaml), Vercel by [`frontend/vercel.json`](../../frontend/vercel.json), and the backend deploy is gated by GitHub Actions.

## Provider comparison

| Area | Selected | Alternative considered | Decision reason |
|---|---|---|---|
| Database | Render PostgreSQL for a free evaluation | Railway PostgreSQL, Neon, Supabase | It integrates with the Render Blueprint and injects private connection properties. The free database is capped at 1 GB and expires after 30 days, so it is not a durable long-term production database. |
| Backend | Render Web Service, Docker runtime | Railway service | Native Git/Blueprint integration, HTTP health check, deploy API, and rollback support. Free web services sleep after inactivity and have an ephemeral filesystem. |
| Frontend | Vercel Hobby static deployment | Netlify, Render Static Site | Git-connected Vite deployment, preview URLs, build-time `VITE_API_URL`, and SPA rewrite support. |
| Poster storage | Cloudflare R2 through the S3 API | Render filesystem, local disk, Cloudinary | The backend keeps the `MediaStorageService` abstraction and stores generated object URLs. Render free files are lost on restart/redeploy; R2 has no egress charge and supports S3-compatible APIs. |

## Security and operational choices

- Production always uses `SPRING_PROFILES_ACTIVE=prod`.
- `JWT_SECRET`, database credentials, exact CORS origin, R2 credentials and public media URL are provider secrets; they are never placed in images, Git, or workflow logs.
- Swagger/OpenAPI is disabled in production. `/actuator/health` returns only `{"status":"UP"}`.
- Hikari is capped at three connections because free-tier databases have finite connection budgets.
- The frontend Vite bundle receives `VITE_API_URL` at build time. Changing the API URL requires a new frontend build.
- Uploaded poster files use generated UUID keys. MIME, decoded image format, dimensions and size are validated before storage.

## Free-tier caveats

Render documents that free web services spin down after 15 minutes, lose local filesystem changes on restart, and that free PostgreSQL expires after 30 days. Railway currently offers a time-limited trial and usage-based plans rather than a permanent zero-cost production database. Vercel Hobby is appropriate for a portfolio SPA within its published usage limits. R2 pricing and limits can change, so the account dashboard remains the source of billing truth.

This means the selected free topology is suitable for a review/demo deployment after credentials are supplied. It is not a claim of durable production availability. For a long-lived public demo, move PostgreSQL to a non-expiring managed plan and keep R2 or another persistent object store.

## Migration path

1. Create a non-expiring managed PostgreSQL database and set the same JDBC variables; Flyway runs V1 through the current migration automatically.
2. Keep the backend image and `ObjectStorageMediaStorageService`; change only endpoint, bucket and credential variables for another S3-compatible provider.
3. Keep the Vercel project and change `VITE_API_URL` if the backend hostname changes.
4. If the frontend provider changes, preserve the SPA fallback and set the new HTTPS origin in `CORS_ALLOWED_ORIGINS`.

## Sources

- [Render free services and PostgreSQL limits](https://render.com/docs/free)
- [Render Blueprint reference](https://render.com/docs/blueprint-spec)
- [Render PostgreSQL connection guidance](https://render.com/docs/postgresql-creating-connecting)
- [Vercel Vite deployment and SPA rewrites](https://vercel.com/docs/frameworks/frontend/vite)
- [Cloudflare R2 pricing](https://developers.cloudflare.com/r2/pricing/)
- [Cloudflare R2 S3-compatible Java client](https://developers.cloudflare.com/r2/examples/aws/aws-sdk-java/)
