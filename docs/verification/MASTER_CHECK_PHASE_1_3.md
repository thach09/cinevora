# Cinevora — Master Verification Report (Phase 0–3)

**Ngày kiểm tra:** 2026-09-19  
**Commit được kiểm tra:** `26bd3bf` — `fix phase 3 stabilization issues`  
**Branch:** `main` — đồng bộ với `origin/main`  
**Phạm vi:** Database/Flyway, Backend Spring Boot/JWT/API, Frontend React/Vite và các luồng tích hợp local.

## Kết luận

Phase 1, Phase 2 và Phase 3 đang ở trạng thái **PASS theo bằng chứng thực thi hiện có**.

Master check đã chạy lại được các kiểm tra offline, Docker/PostgreSQL smoke, backend tests, backend runtime với PostgreSQL local, API authorization, CRUD smoke, frontend audit, frontend production build và Vite runtime. Một phần smoke test hiện không đạt do database đang giữ dữ liệu probe/manual từ các lần test trước, không phải do schema/FK/BCrypt/index bị hỏng. Browser target không được expose cho lượt này nên không tạo thêm được bằng chứng browser-console mới.

1. Database hiện không ở trạng thái fresh nên ST2 và ST5 không thể kỳ vọng giá trị seed/identity ban đầu.
2. Browser target không được expose cho Computer Use (`iab unavailable`); HTTP SPA runtime và production build vẫn PASS.

## Trạng thái repo và môi trường

Tại thời điểm bắt đầu master check:

```text
## main...origin/main
```

Working tree sạch. Sau khi tạo báo cáo này, thay đổi mới duy nhất dự kiến là file báo cáo này.

Môi trường thực tế:

```text
Node.js       v24.21.0
npm           11.19.0
Java          21.0.12.1 LTS
Maven         3.9.16
Spring Boot   3.4.5
PostgreSQL    16.15 (xác nhận qua backend runtime)
Vite          6.4.3 (package-lock)
React Router  7.18.4 (package-lock)
```

## Phase 0 — Cleanup và chuẩn bị repo

| Kiểm tra | Kết quả | Bằng chứng |
|---|---:|---|
| Monorepo có `backend/`, `frontend/`, `legacy-cli/`, `docs/`, `tools/` | PASS | `git status`, `rg --files` |
| Plaintext seed data không còn trong working tree | PASS | Phase 1 verifier: `Plaintext leak : 742 file quet, 0 leak` |
| Có thể truy nguyên `legacy-cli/data/users.txt` từ Git history | PASS | Phase 1 verifier tìm thấy commit `078b5114d9b1617572b56062fc70147f4ac95bfc` |
| Không sửa `legacy-cli/` trong các commit Phase 1–3 | PASS | `git show --name-status` và source tree hiện tại |

## Phase 1 — Database và Flyway

### 1. Kiểm tra offline

Đã chạy:

```powershell
C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe `
  -NoProfile -ExecutionPolicy Bypass `
  -File .\tools\verify-phase1.ps1
```

Kết quả thực tế:

```text
[PASS] V1 CREATE TABLE : 7
[PASS] V1 FOREIGN KEY : 9
[PASS] V1 ON DELETE RESTRICT : 1
[PASS] V1 UNIQUE INDEX : 1
[PASS] V2 rows categories : 7
[PASS] V2 rows movies : 60
[PASS] V2 rows users : 11
[PASS] V2 rows watchlist : 7
[PASS] V2 rows favourites : 4
[PASS] V2 rows watch_history : 9
[PASS] V2 rows continue_watching : 6
[PASS] V2 so lan xuat hien BCrypt hash : 11
[PASS] Plaintext leak : 742 file quet, 0 leak
[PASS] no BOM : ...

KET QUA: PASS - tat ca kiem tra offline deu thanh cong
```

### 2. Database runtime

Trong lần chạy backend runtime mới nhất:

```text
HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection
Database: jdbc:postgresql://localhost:5432/cinevora_db (PostgreSQL 16.15)
Successfully validated 2 migrations
Current version of schema "public": 2
Schema "public" is up to date. No migration necessary.
```

Docker Desktop đã được xác nhận bằng binary path user cung cấp:

```text
Client Version: 29.8.0
Context: desktop-linux
Compose plugin: v5.5.1
Server Version: 29.8.0
Containers: 2 (Running: 1)
Operating System: Docker Desktop
```

Sau đó `tools/verify-st.ps1` được chạy lại với Docker binary path này. Kết quả:

```text
ST1: PASS  flyway_schema_history = '1:1:t 2:2:t'
ST2: FAIL cat/mov/usr/wl/fav/wh/cw = '9/63/11/7/5/11/6'
ST3: PASS orphans = '0/0/0/0/0'
ST5a: FAIL users.id ke tiep = '45' (ky vong 12, fresh DB)
ST5b: FAIL movies.id ke tiep = '97' (ky vong 61, fresh DB)
ST4a: PASS unique username constraint
ST4b: PASS foreign key constraint
ST4c: PASS percent CHECK constraint
ST6: PASS 8 required indexes
ST7: PASS BCrypt = '11/11/11/1'
ST8: PASS Unicode + LOWER(username) = 'true/admin/true'

KET QUA: 8 PASS, 3 FAIL
```

ST2/ST5 fail vì database đang giữ các bản ghi đã archive hoặc được tạo trong manual smoke trước đó: tổng số hiện tại lớn hơn seed và sequence đã tăng. Script itself cũng ghi rõ ST5 chỉ dùng cho DB fresh; việc reset volume sẽ xóa dữ liệu local hiện tại nên master check không tự động thực hiện destructive reset.

Runbook và Phase 1 documentation ghi nhận trước đó đã chạy thật PostgreSQL 16.15 qua Docker với **8/8 smoke test PASS** trên database fresh. Backend runtime hiện tại tiếp tục xác nhận database local reachable, schema Flyway version 2 hợp lệ và JPA khởi tạo thành công.

## Phase 2 — Backend Spring Boot

### 1. Build và unit tests

Đã chạy `mvn.cmd test` với Maven Central access cần thiết. Kết quả:

```text
[INFO] Running com.cinevora.service.AuthServiceTest
[INFO] Tests run: 1, Failures: 0, Errors: 0
[INFO] Running com.cinevora.service.CategoryServiceTest
[INFO] Tests run: 1, Failures: 0, Errors: 0
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Đã package artifact hiện tại bằng `mvn.cmd package -DskipTests`:

```text
[INFO] Replacing main artifact ... cinevora-backend-0.1.0-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

### 2. Backend startup, Flyway và API runtime

Artifact mới được chạy trên port `18080`. Startup thành công với:

```text
Tomcat started on port 18080 (http)
Started CinevoraApplication in 7.494 seconds
Application availability state ... ACCEPTING_TRAFFIC
```

HTTP smoke thực tế:

| Luồng | Kết quả |
|---|---:|
| `GET /actuator/health` | HTTP 200 |
| `GET /swagger-ui.html` | HTTP 200 sau redirect nội bộ |
| `GET /v3/api-docs` | HTTP 200 |
| OpenAPI version | `3.0.1` |
| Bearer JWT scheme | Có `bearerAuth` |
| OpenAPI paths | 19 |
| `GET /api/v1/movies?page=0&size=2` | HTTP 200 |
| Customer login | success, role `CUSTOMER` |
| Admin login | success, role `ADMIN` |
| Anonymous `GET /api/v1/statistics` | HTTP 403 |
| Customer `GET /api/v1/users/me/watchlist` | HTTP 200 |
| Customer `POST /api/v1/categories` | HTTP 403 |
| Admin `GET /api/v1/categories` | HTTP 200 |
| CORS preflight từ `http://localhost:5173` | HTTP 200, trả `Access-Control-Allow-Origin` đúng origin |

Các read flows được gọi lại bằng API thật và đều HTTP 200:

```text
Categories: HTTP 200
Browse: HTTP 200
Search: HTTP 200
Trending: HTTP 200
Watchlist: HTTP 200
Favourites: HTTP 200
History: HTTP 200
ContinueWatching: HTTP 200
HistoryExport: HTTP 200
Statistics: HTTP 200
```

### 3. CRUD smoke thật

Đã tạo temporary records bằng tài khoản ADMIN, sau đó update và archive:

```text
Category create: HTTP 200, id=9
Category update: HTTP 200
Movie create: HTTP 201, id=96
Movie update: HTTP 200
Movie archive: HTTP 200
Category archive: HTTP 200
```

Các user-data mutation cũng đã test với movie id `42` và dọn trạng thái sau test:

```text
Watchlist add: HTTP 200
Watchlist remove: HTTP 200
Favourite add: HTTP 200
Favourite remove: HTTP 200
Continue update: HTTP 200
Continue remove: HTTP 200
```

### 4. Source review các fix quan trọng

- `CinevoraApplication` exclude `UserDetailsServiceAutoConfiguration`, không còn Spring generated default password ngoài ý muốn.
- `OpenApiConfig` khai báo HTTP Bearer JWT scheme `bearerAuth` và global security requirement.
- `SecurityConfig` bật `@EnableMethodSecurity`, phân quyền `hasRole('ADMIN')`, public GET movie/category, CORS origin đọc từ `CORS_ALLOWED_ORIGINS` với credentials.
- `GlobalExceptionHandler` log `Unhandled exception` bằng `log.error("Unhandled exception", ex)` và trả `ApiResponse` thống nhất.
- API sử dụng prefix `/api/v1` và DTO/response wrapper tương ứng.

## Phase 3 — Frontend React

### 1. Dependency và audit

Package lock hiện tại xác nhận Vite `6.4.3`, React Router `7.18.4`, React 18 và các dependency Phase 3.

Đã chạy:

```powershell
npm.cmd audit --audit-level=high
```

Kết quả:

```text
found 0 vulnerabilities
```

### 2. Production build

Đã chạy:

```powershell
npm.cmd run build
```

Kết quả thực tế:

```text
vite v6.4.3 building for production...
✓ 168 modules transformed.
dist/index.html                   0.57 kB
dist/assets/index-DxuBeN2s.css   36.60 kB
dist/assets/index-DAZBq1CO.js   410.70 kB
✓ built in 1.90s
```

### 3. Frontend runtime

Đã start Vite dev server:

```text
VITE v6.4.3  ready in 264 ms
➜  Local: http://127.0.0.1:5173/
```

HTTP smoke:

```text
HTTP 200
CONTENT-TYPE text/html
```

Source review xác nhận:

- React Router routes cho login/register, protected customer routes và admin routes.
- Zustand giữ JWT/user session.
- Axios tự gắn `Authorization: Bearer ...` và logout khi nhận 401.
- TanStack Query quản lý fetching/loading/error/cache.
- `Input` và `Select` dùng `forwardRef`, tương thích React Hook Form.
- Layout có Sidebar/Navbar/Footer, responsive mobile, loading/error/empty/toast states.
- Customer flows: browse, search, detail, watchlist, favourites, history, continue watching.
- Admin flows: dashboard, category CRUD, movie CRUD, statistics.

Browser target không khả dụng trong Computer Use session (`iab unavailable`, không có browser inventory), nên không thực hiện lại được thao tác click/login hoặc đọc console browser trong lượt master này. Đây là giới hạn công cụ, không phải build/runtime failure của frontend.

## Các cảnh báo còn lại

1. **Database local không còn fresh**: ST2/ST5 của script seed verifier sẽ chỉ PASS lại sau khi reset volume và migrate fresh. Master check không reset dữ liệu vì đó là thao tác xóa material data.
2. **npm audit/build cần elevated access** vì registry và cache `node_modules` bị sandbox/permission giới hạn ở lần chạy thường. Khi chạy với access phù hợp, cả audit và build đều PASS.
3. **Backend test suite hiện có 2 unit test classes / 2 test cases** (`AuthServiceTest`, `CategoryServiceTest`). Kết quả là PASS, nhưng chưa nên diễn giải thành coverage 100% hoặc full integration-test coverage.
4. Mockito phát cảnh báo self-attaching Java agent trên JDK mới. Đây là warning tương thích tương lai, không làm test fail.
5. `AGENTS.md` vẫn còn dòng tiến độ cũ ghi Phase 2 “chưa bắt đầu”; source code, commit và bằng chứng runtime hiện tại đã vượt qua trạng thái đó. Báo cáo này là snapshot verification hiện hành.

## Verdict cuối

```text
PHASE 1 — PASS (fresh historical smoke 8/8; current state 8 PASS / 3 state-dependent FAIL)
PHASE 2 — PASS
PHASE 3 — PASS

MASTER CHECK — PASS WITH ENVIRONMENT CAVEATS
```

Không phát hiện lỗi source/blocker mới trong lượt kiểm tra này. Các record temporary tạo trong CRUD smoke đã được archive; watchlist/favourite/continue-watching test state đã được remove.
