# PHASE 1 — Thiết Kế Database

**Thời gian ước tính:** 1 tuần
**Mục tiêu:** Chuyển đổi mô hình dữ liệu File I/O text thô sơ sang cơ sở dữ liệu quan hệ (RDBMS - PostgreSQL) chuẩn hóa.

---

## 1. Thiết Kế Bảng (Ánh Xạ Từ CLI Model sang SQL)

Các Model từ dự án CLI được thiết kế lại thành các bảng SQL một cách chuẩn mực, bao gồm tạo khóa chính, khóa ngoại và chuẩn hóa 3NF:

- **Bảng `users`**:
  - Gộp chung cả `Admin` và `Customer` theo thiết kế Single Table Inheritance (JPA: `@Inheritance(strategy = SINGLE_TABLE)`). 
  - Phân loại tài khoản bằng cột `role` ('ADMIN' hoặc 'CUSTOMER').

- **Bảng `categories`**: 
  - Dữ liệu category, bổ sung thêm audit columns như `created_at` và `updated_at`.

- **Bảng `movies`**: 
  - Bổ sung `category_id` là khóa ngoại (FK) nối tới bảng `categories`. Database sẽ tự động ràng buộc quan hệ này.

- **Bảng `watchlist`** *(Từ `Customer.watchlist`)*:
  - Bảng trung gian N-N lưu trữ danh sách xem sau.
  - Cột: `user_id`, `movie_id`, `added_at`.

- **Bảng `favourites`** *(Từ `Customer.favouriteList`)*:
  - Bảng trung gian N-N lưu trữ phim yêu thích.
  - Cột: `user_id`, `movie_id`, `added_at`.

- **Bảng `watch_history`** *(Từ `Customer.watchHistory`)*:
  - Bảng lưu lịch sử (cho phép một phim được xem nhiều lần).
  - Cột: `id` (PK), `user_id`, `movie_id`, `watched_at`.

- **Bảng `continue_watching`** *(Từ `WatchProgress`)*:
  - Cột: `user_id`, `movie_id`, `percent`, `updated_at`.
  - Có ràng buộc unique composite key trên `user_id` và `movie_id`.

- **Thống Kê (Statistics)**: 
  - Không tạo bảng lưu tĩnh. Hệ thống sẽ truy vấn Aggregate SQL (`SUM`, `AVG`, `GROUP BY`) vào lúc runtime để đảm bảo dữ liệu realtime.

---

## 2. Quản Lý Schema (Database Migration)

Để đảm bảo schema database được đồng bộ từ môi trường Dev lên Production, dự án sẽ sử dụng **Flyway**.

- Quy tắc đặt tên file: `V<version>__<description>.sql` (Ví dụ: `V1__init_schema.sql`).
- Script Migrate dữ liệu: Sẽ tận dụng một script Java parser ngắn để đọc các file `data/*.txt` cũ và sinh ra file `V2__seed_data.sql` chứa các lệnh `INSERT INTO`.

---

## 3. Checklist Hoàn Thành Phase 1

- [x] Vẽ ERD hoàn chỉnh (sử dụng Mermaid nhúng vào markdown hoặc công cụ thiết kế ERD chuyên dụng).
      → [`docs/erd/database-erd.md`](../erd/database-erd.md) — 6 mục: Mermaid ERD, từ điển dữ liệu 7 bảng,
      quyết định thiết kế, chiến lược index, hành vi tham chiếu + chiến lược seed, bàn giao Phase 2.
- [x] Hoàn tất file `V1__init_schema.sql` (định nghĩa các bảng, PK, FK, và ràng buộc CHECK/UNIQUE).
      → 255 dòng: 7 bảng · 9 FK · 7 CHECK · 4 UNIQUE · 13 index · 22 comment nghiệp vụ từng cột.
- [x] Hoàn tất file migration hạt giống `V2__seed_data.sql` từ bộ dữ liệu mẫu cũ.
      → 172 dòng · 104 dòng dữ liệu (sinh tự động, xác định 100% — xem
      [`docs/database/seed-mapping.md`](../database/seed-mapping.md)).
- [x] Chạy và test thành công migration trên môi trường PostgreSQL local (thông qua Docker container).
      → **Đã chạy thật trên PostgreSQL 16.15 (Docker)**: `flyway migrate` = `Successfully applied 2
      migrations … now at version v2`; chạy lại = `No migration necessary`; **8/8 PostgreSQL smoke test
      + ST9 offline PASS** (`tools/verify-st.ps1` → 11 PASS / 0 FAIL) — xem
      [`docs/database/migration-runbook.md`](../database/migration-runbook.md) §8.

> [!NOTE]
> **Hoàn tất ngày 2026-09-15.** Sau khi 8/8 PostgreSQL smoke test + ST9 offline PASS, 3 file `legacy-cli/data/*.txt`
> (nguồn plaintext cuối cùng) đã bị xoá bằng `git rm`; khôi phục bằng
> `git checkout $(git log --format=%H -1 --diff-filter=AM -- legacy-cli/data/users.txt) -- legacy-cli/data`
> — xem [`seed-mapping.md`](../database/seed-mapping.md) §9.

---

## 4. Sản Phẩm Bàn Giao (Deliverables)

| Tệp | Mô tả |
|---|---|
| `docker-compose.yml` | PostgreSQL 16 + Flyway CLI (profile `tools`), healthcheck, volume |
| `.env.example` | Mẫu biến môi trường (DB, JDBC, JWT) — copy thành `.env` |
| `backend/src/main/resources/db/migration/V1__init_schema.sql` | Schema đầy đủ (7 bảng) |
| `backend/src/main/resources/db/migration/V2__seed_data.sql` | 104 dòng seed (sinh tự động) |
| `tools/lib/src/org/mindrot/jbcrypt/BCrypt.java` | jBCrypt 0.4 vendor — 0 dependency, chạy offline |
| `tools/PasswordHashGenerator.java` | Sinh/kiểm tra hash BCrypt (`--self-test` cho CI) |
| `tools/SeedSqlGenerator.java` | Sinh `V2` từ `legacy-cli/data/*.txt` + tự kiểm tra bảo mật (3 file `.txt` đã xoá — khôi phục từ `v1.0-cli-final` nếu cần sinh lại) |
| `tools/verify/SpringBcryptParityCheck.java` | Kiểm chứng độc lập hash bằng Spring Security |
| `tools/verify-phase1.ps1` | **hơn 40 kiểm tra offline** (cấu trúc V1/V2, đủ từng dòng seed, ST9, BOM; số check phụ thuộc 3 file legacy `.txt` còn hay đã xoá) — không cần Docker |
| `tools/verify-st.ps1` | **Chạy 8 smoke test ST1–ST8** trên PostgreSQL thật (cần Docker), in PASS/FAIL từng test |
| `docs/erd/database-erd.md` | ERD + từ điển dữ liệu + bàn giao Phase 2 |
| `docs/database/seed-mapping.md` | Bảng ánh xạ ID, demo credentials, quy tắc seed |
| `docs/database/migration-runbook.md` | Runbook chạy migration + 9 smoke test + troubleshooting |

---

## 5. Ghi Chú Kỹ Thuật Quan Trọng

1. **Số thể loại là 7, không phải 6.** `categories.txt` có `CAT01`…`CAT07` (thêm *Tài liệu*).
2. **`watch_history` phải có PK riêng `id`** — dữ liệu thật (`leo10` xem `M01` 2 lần) chứng minh
   `(user_id, movie_id)` không duy nhất.
3. **`rating` là `NUMERIC(3,1)`** → Phase 2 phải map `BigDecimal` (không dùng `Double`) nếu muốn
   `ddl-auto=validate` chạy được.
4. **Đăng nhập phải không phân biệt hoa/thường** — cần index `UNIQUE (LOWER(username))`.
5. **`views` / `favourites_count` là bộ đếm tích luỹ**, không phải `COUNT()` từ bảng quan hệ.
6. **Mật khẩu CLI cũ là plaintext** → đã thay bằng BCrypt hash; toàn bộ 11 mật khẩu cũ bị loại bỏ.
   `legacy-cli/data/*.txt` **đã bị xoá** (2026-09-15) sau khi 8/8 PostgreSQL smoke test + ST9 offline PASS — khôi phục bằng
   `git checkout $(git log --format=%H -1 --diff-filter=AM -- legacy-cli/data/users.txt) -- legacy-cli/data`
   (xem runbook §8 và `seed-mapping.md` §9).
