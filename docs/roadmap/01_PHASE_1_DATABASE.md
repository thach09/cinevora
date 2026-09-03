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

- [ ] Vẽ ERD hoàn chỉnh (sử dụng Mermaid nhúng vào markdown hoặc công cụ thiết kế ERD chuyên dụng).
- [ ] Hoàn tất file `V1__init_schema.sql` (định nghĩa các bảng, PK, FK, và ràng buộc CHECK/UNIQUE).
- [ ] Hoàn tất file migration hạt giống `V2__seed_data.sql` từ bộ dữ liệu mẫu cũ.
- [ ] Chạy và test thành công migration trên môi trường PostgreSQL local (thông qua Docker container).
