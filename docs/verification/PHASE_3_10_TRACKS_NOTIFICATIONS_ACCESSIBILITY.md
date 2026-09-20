# Phase 3.10 — Tracks, Notifications & Accessibility

Status: **implemented and runtime-verified locally**

## Delivered

- Added additive Flyway V7 tables for movie subtitle/audio tracks and user notifications; binary media remains outside PostgreSQL.
- Added public active-track API and admin media-track CMS API with archive behavior.
- Extended the native HTML5 player with native WebVTT subtitle tracks and alternate audio-source selection while retaining progress save semantics.
- Added profile-independent account notification inbox with unread count, mark-one-read and mark-all-read actions.
- Added welcome notifications for existing active users during migration; notification ownership is enforced by the authenticated user id.
- Added a notification center in the authenticated layout with unread badge, keyboard-accessible buttons and action links.
- Added a dedicated admin Media workspace for track management.
- Added skip-to-content navigation, main landmark id, explicit accessible labels and focus-visible styling.

## Chosen logic

Tracks are metadata rows referencing external or object-storage URLs, so the database does not become a binary media store. Public playback only receives active rows; admin can archive a track without losing the movie relationship. Subtitle tracks map directly to native `<track>` elements. Audio variants use the same HTML5 player and restore the saved watch position when the selected source changes.

Notifications are user-owned and queried with a bounded latest-20 inbox plus an indexed unread count. Mark/read operations always resolve by `(notification_id, authenticated_user_id)`, preventing cross-account access. The frontend uses an initial empty inbox state so the layout remains stable while the request is loading.

## Evidence

```text
Backend startup
Flyway: Successfully validated 7 migrations
Flyway: Migrating schema "public" to version "7 - tracks and notifications"
Flyway: Successfully applied 1 migration ... now at version v7
Hibernate: EntityManagerFactory initialized successfully
Spring Boot: Tomcat started on port 18080

mvn.cmd -q test
Process exit code: 0

mvn.cmd -q -DskipTests package
Process exit code: 0

Phase 3.10 API smoke
{"loginToken":true,"subtitleKind":"SUBTITLE","audioKind":"AUDIO","publicTrackCount":2,"adminTrackCount":2,"notificationCount":1,"unreadBefore":1,"notificationReadStatus":200,"unauthAdminTrackStatus":403,"cleanup":"ok"}

npm.cmd run lint
tsc -b --pretty false
Process exit code: 0

npm.cmd run build
vite v6.4.3
✓ 171 modules transformed.
✓ built in 1.89s

Frontend route smoke via Vite 6.4.3
200 /browse
200 /search
200 /movies/42
200 /continue-watching
200 /admin/media
200 /admin/archive
200 /account
```

The Windows computer-use surface had no browser available, so visual browser inspection and console capture remain unverified. API ownership, migration, runtime, TypeScript, production build and route checks passed.
