# Phase 3.11 — Performance, SEO & Feature Freeze

**Ngày kiểm tra:** 2026-09-21  
**Trạng thái:** PASS cho backend/runtime và production build; visual browser smoke test chưa thực hiện được vì môi trường hiện tại không có browser surface.

**Commit implementation:** backend `5d304ba`, frontend `b395f5f`.

## Phạm vi hoàn thành

- Thêm Flyway V8 với các index phục vụ các đường nóng: active movies theo views, active users theo created time, watch history theo profile/movie, movie preferences theo movie/signal và search history theo query/time.
- Tách route frontend bằng `React.lazy` và `Suspense`; màn hình theo nhóm chỉ được tải khi người dùng mở route tương ứng.
- Thêm lớp SEO dùng chung cho title, description, Open Graph, canonical URL và JSON-LD Movie detail.
- Thêm `robots.txt`, các metadata nền trong `index.html`, đồng thời giữ route private/admin ngoài chỉ mục tìm kiếm.
- Giữ nguyên ưu tiên UX của CINEVORA: tìm kiếm nhanh, category rõ, poster dễ quét, trạng thái phim rõ, Continue Watching dễ thấy và CTA có nhãn hành động cụ thể.

## Bằng chứng kiểm thử thực tế

### Backend runtime và migration

Log khởi động thực tế:

```text
Flyway: Successfully validated 8 migrations
Current version schema public: 7
Migrating schema "public" to version "8 - performance indexes"
Successfully applied 1 migration ... now at version 8
Hibernate initialized EntityManagerFactory
Tomcat started on port 18080
Started ... in 18.203s
```

### Backend test và API smoke

- `mvn.cmd -q test`: exit code `0`.
- Cảnh báo Mockito/Byte Buddy chỉ là cảnh báo tương thích JDK tương lai, không có test failure.
- Smoke chạy trên backend local với database thật:

```json
{"health":"UP","loginToken":true,"movies":200,"movieCount":6,"categories":200,"categoryCount":7,"suggestions":200,"suggestionCount":1,"home":200,"recommendations":200,"recommendationCount":6,"preferences":200,"notifications":200,"notificationCount":1,"adminUsers":200,"adminMovies":200,"tracks":200,"trackCount":0,"refresh":200,"unauthHome":403}
```

- CORS preflight: status `200`, origin `http://localhost:5173`, methods `GET,POST,PUT,PATCH,DELETE,OPTIONS`, headers gồm `authorization, x-profile-id, content-type`.

### Frontend build và route smoke

- `npm.cmd exec tsc -- --noEmit --pretty false`: exit code `0`.
- `npm.cmd run build`: exit code `0`.
- Vite `6.4.3`, `173 modules transformed`.
- Entry bundle sau code-splitting: `300.25 kB`, gzip `99.01 kB`; CSS gzip `8.94 kB`.
- Các chunk route chính được sinh riêng: Browse, MovieDetail, Admin, Account, Library, Auth.
- Route smoke với Vite local:

```text
/robots.txt          200 text/plain 150 bytes
/                    200 text/html  943 bytes
/browse              200 text/html  943 bytes
/search              200 text/html  943 bytes
/movies/42           200 text/html  943 bytes
/continue-watching   200 text/html  943 bytes
/admin/media         200 text/html  943 bytes
/account             200 text/html  943 bytes
```

- `git diff --check`: không phát hiện whitespace error; chỉ có cảnh báo chuyển LF/CRLF của Git trên Windows.

## SEO và giới hạn còn lại

Technical SEO baseline đã có: title/description động, canonical URL, Open Graph, JSON-LD cho Movie detail và robots policy cho vùng private/admin. Tuy nhiên frontend hiện là SPA và các trang phim vẫn nằm sau authentication, vì vậy đây chưa phải full crawlable SEO như SSR/prerender.

Đề xuất cho giai đoạn sau: public movie detail route có dữ liệu server-rendered/prerender, sitemap sinh từ catalog active, Lighthouse trên thiết bị thật và kiểm tra structured data bằng công cụ crawler thực tế.

## Kết luận phase

Phase 3.11 đạt mục tiêu performance/SEO baseline và đủ điều kiện feature freeze theo roadmap. Không phát hiện lỗi compile, migration, API smoke, CORS hoặc production build. Visual regression và browser interaction thực tế vẫn cần chạy trong môi trường có Chrome/Playwright trước khi phát hành production.
