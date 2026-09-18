# PHASE 2 — Xây Dựng Backend (Spring Boot)

**Thời gian ước tính:** 2–3 tuần
**Mục tiêu:** Chuyển đổi toàn bộ Core Business Logic từ mã CLI thành các chuẩn REST API, kết nối DB và cấu hình bảo mật.

---

## 1. Khởi Tạo Dự Án

- **Base Framework:** Spring Boot 3.x (Java 21 LTS).
- **Dependencies (Spring Initializr):** `Spring Web`, `Spring Data JPA`, `PostgreSQL Driver`, `Spring Security`, `Validation`, `Flyway Migration`, `Lombok`, `springdoc-openapi`.

---

## 2. Lộ Trình Chuyển Đổi Các Tầng (Layers)

Chuyển đổi theo hướng Bottom-Up (từ dưới lên) để không làm vỡ các dependency:

1. **Entity Layer**: Chuyển các object từ `model/` sang. Bổ sung các Annotation JPA (`@Entity`, `@Id`, `@Column`). Giữ nguyên các rule validate trong setter, đồng thời bổ sung Bean Validation (`@NotBlank`, `@Min`,...).
2. **Repository Layer**: Thay thế interface tự code bằng `extends JpaRepository<T, ID>`. Tạo các Custom Query Method (e.g. `findByIsActiveTrue`) thay thế cho logic duyệt vòng lặp thủ công trong `SearchUtils`.
3. **Service Layer**: Bê nguyên logic nghiệp vụ từ các Controller CLI cũ sang đây. Đổi cách gọi lệnh Persistence, ví dụ thay vì `saveAll(movieList)` trên file text thì gọi `movieRepository.save(movie)`.
4. **DTO Layer**: Thiết kế bộ DTO riêng cho Request và Response để tránh lộ lọt Entity (đặc biệt che giấu trường `password` của User).
5. **Controller Layer (REST)**: Viết các REST API hoàn toàn mới nhận JSON request, gọi hàm ở Service và trả về `ResponseEntity<T>`.
6. **Security Layer**: Cấu hình bộ lọc Spring Security, triển khai JWT Authentication Filter, phân quyền `@PreAuthorize` theo Role.
7. **Exception Layer**: Dùng `@ControllerAdvice` để gom và bắt lỗi tập trung (bao gồm cả các `ValidationException` cũ), trả ra mã lỗi HTTP 400 cùng với custom message.

---

## 3. Thiết Kế Endpoints (API Sơ Bộ)

| Nhóm | Method | Endpoint | Chức Năng |
|---|---|---|---|
| **Auth** | POST | `/api/auth/login` | Đăng nhập tài khoản, trả về JWT Token |
| **Auth** | POST | `/api/auth/register` | Đăng ký tài khoản Customer |
| **Category** | GET/POST/PUT/DELETE | `/api/categories` | Các luồng CRUD Category (Admin only) |
| **Category** | PATCH | `/api/categories/{id}/restore` | Khôi phục Category đã xóa mềm |
| **Movie** | GET | `/api/movies` | Lấy danh sách phim + Filter bằng query params |
| **Movie** | GET | `/api/movies/{id}` | Chi tiết 1 phim |
| **Movie** | POST/PUT/DELETE | `/api/movies` | CRUD thông tin Phim (Admin only) |
| **User Data**| GET/POST/DELETE | `/api/users/me/watchlist` | Quản lý Watchlist cá nhân |
| **User Data**| GET/POST/DELETE | `/api/users/me/favourites` | Quản lý Favourites cá nhân |
| **User Data**| GET/POST | `/api/users/me/history` | Lịch sử xem phim |
| **User Data**| GET/PUT/DELETE | `/api/users/me/continue-watching` | Tiến trình xem phim |
| **Report** | GET | `/api/users/me/history/export` | Trả file xuất CSV |
| **Stats** | GET | `/api/statistics` | Báo cáo phân tích hệ thống (Admin) |

---

## 4. Bảo Mật & Testing

- **Mật khẩu:** Encode dữ liệu mẫu thành chuỗi hash `BCrypt` trước khi nạp vào DB, tránh lưu plaintext.
- **Testing:** Viết các Unit Test (JUnit 5 + Mockito) để verify lại 100% nghiệp vụ (Ví dụ: Không được phép xóa Category khi vẫn còn Movie đang liên kết). Test Controller tích hợp với Testcontainers.

---

## 5. Checklist Hoàn Thành Phase 2

- [x] Khởi tạo thành công thư mục `backend/` với Spring Boot và chạy được.
- [x] Kết nối Database PostgreSQL (chạy Docker/local PostgreSQL) và chạy Flyway tự động tạo bảng.
- [x] Hoàn thành Entity và Repository Layer cho 7 bảng dữ liệu.
- [x] Hoàn thành Service Layer và pass bộ Unit Test bảo vệ business logic.
- [x] Hoàn thành toàn bộ REST Controller Endpoint, mapping với Request/Response DTO.
- [x] Spring Security + JWT hoạt động (đăng nhập thành công, token truy cập được router bảo vệ).
- [x] Chạy được trang tài liệu Swagger UI tại `/swagger-ui.html`.

### Trạng thái triển khai trong repository

Mã nguồn Phase 2 đã được triển khai trong `backend/`: Maven/Spring Boot 3.4.5,
Java 21, JPA entity + repository cho 7 bảng, service và DTO, REST API `/api/v1`,
BCrypt/JWT security, Flyway profiles, Swagger/OpenAPI, CSV export và unit test
cho các business rule cốt lõi.

### Bằng chứng verify mới nhất — 2026-09-19

- `mvn.cmd test`: 2 tests, 0 failures, 0 errors — BUILD SUCCESS.
- `mvn.cmd package -DskipTests`: BUILD SUCCESS.
- Backend runtime trên port `18080`: Tomcat start thành công, kết nối PostgreSQL 16.15, Flyway validate 2 migration và schema version 2 up-to-date.
- `GET /actuator/health`, Swagger UI, OpenAPI `3.0.1` với `bearerAuth`: HTTP 200.
- ADMIN/CUSTOMER login, protected authorization, CORS preflight, browse/search/trending, user libraries, statistics và temporary Category/Movie CRUD: PASS.

Test suite hiện có 2 unit test cases; chưa nên diễn giải thành full integration-test coverage. Chi tiết log nằm trong [`MASTER_CHECK_PHASE_1_3.md`](../verification/MASTER_CHECK_PHASE_1_3.md).
