# Security & API Standards (Cross-Cutting Concerns)

**Phạm vi áp dụng:** Xuyên suốt toàn bộ dự án (Cross-cutting concerns across Phase 1 – Phase 5)  
**Mục tiêu:** Thiết lập các quy chuẩn kỹ thuật đồng nhất về bảo mật, chuẩn hóa API RESTful, quản lý môi trường cấu hình và tối ưu tài nguyên hạ tầng cloud ngay từ đầu nhằm hạn chế phát sinh lỗi khi tích hợp.

---

## 1. CORS Configuration Theo Môi Trường

Khi tách biệt Frontend (Vercel/Netlify) và Backend (Render/Railway), trình duyệt sẽ kích hoạt cơ chế Same-Origin Policy và gửi preflight request (`OPTIONS`). Cấu hình CORS không chuẩn là một trong những lỗi phổ biến nhất làm gián đoạn quá trình demo.

### 1.1. Nguyên Tắc Cấu Hình
- **Không hardcode domain** trực tiếp trong source code Java. Cần đọc `allowed-origins` từ biến môi trường qua file cấu hình `application.yml`:
  ```yaml
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
  ```
- **Tách biệt cấu hình theo môi trường:** Thiết lập giá trị `CORS_ALLOWED_ORIGINS` tương ứng trên dashboard của Render/Railway/Vercel mà không cần build lại Docker image khi domain Frontend thay đổi:
  - *Dev (Local):* `http://localhost:5173`
  - *Staging/Prod:* `https://cinevora.vercel.app` (hoặc domain tùy chỉnh)

### 1.2. Lưu Ý Quan Trọng Về Credentials & Origin Patterns
- **Quy tắc `allowCredentials(true)`:** Hệ thống sử dụng JWT token gửi kèm HTTP header hoặc Cookie xác thực, do đó bắt buộc bật `allowCredentials(true)`. 
  > [!CAUTION]
  > **TUYỆT ĐỐI KHÔNG** được cấu hình `allowedOrigins("*")` khi `allowCredentials(true)` đang được kích hoạt. Spring Framework và chuẩn W3C CORS sẽ ném exception và từ chối khởi động ứng dụng để ngăn ngừa lỗ hổng bảo mật CSRF.

- **Hỗ trợ Vercel Preview Deployments:** Mỗi Pull Request trên Vercel sẽ sinh một URL ngẫu nhiên dạng `https://cinevora-git-<branch>-<username>.vercel.app`. Cần cấu hình `allowedOriginPatterns` thay vì `allowedOrigins` cố định:
  ```java
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
      CorsConfiguration configuration = new CorsConfiguration();
      // Hỗ trợ cả domain production lẫn preview deployments từ Vercel
      configuration.setAllowedOriginPatterns(List.of(
          "http://localhost:5173",
          "https://cinevora-*.vercel.app",
          "https://cinevora.vercel.app"
      ));
      configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
      configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
      configuration.setAllowCredentials(true);
      configuration.setMaxAge(3600L);

      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", configuration);
      return source;
  }
  ```

---

## 2. Hash Mật Khẩu Dữ Liệu Mẫu (BCrypt) Khi Migration

Trong phiên bản CLI cũ, mật khẩu tài khoản người dùng được lưu trữ dưới dạng văn bản thô (plaintext). Khi chuyển sang kiến trúc Web với Spring Security, toàn bộ mật khẩu phải được mã hóa một chiều bằng thuật toán `BCrypt`.

### 2.1. Quy Trình Tạo Dữ Liệu Hạt Giống (Seed Data)
- **Hạn chế của Flyway:** Các file migration của Flyway (`.sql`) là câu lệnh SQL thuần và thực thi độc lập trước khi Spring context khởi tạo, do đó **không thể gọi bean `BCryptPasswordEncoder`** lúc migration.
- **Quy trình chuẩn:**
  1. Viết một utility script độc lập (Java `main` hoặc script Python/Node.js) chạy **MỘT LẦN DUY NHẤT** ngoài chu trình deploy.
  2. Truyền danh sách mật khẩu mẫu vào script để băm (hash) thành chuỗi BCrypt (định dạng `$2a$10$...`).
  3. Dán cứng chuỗi hash (literal string) vào file `V2__seed_data.sql`:
     ```sql
     -- Ví dụ seed user admin với BCrypt hash của password demo (hash thật đang dùng trong V2)
     INSERT INTO users (id, username, email, password, full_name, role, is_active, created_at, updated_at)
     VALUES (1, 'admin', 'admin@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'System Admin', 'ADMIN', true, NOW(), NOW());
     ```

### 2.1.1. Công Cụ Sinh Hash Thực Tế Của Phase 1

Quy trình trên đã được hiện thực hóa thành công cụ chạy được **offline, chỉ cần JDK** (không cần
Maven/Python/Node):

| Công cụ | Vai trò |
|---|---|
| `tools/lib/src/org/mindrot/jbcrypt/BCrypt.java` | jBCrypt 0.4 vendor (0 dependency) |
| `tools/PasswordHashGenerator.java` | Sinh hash mới, `--verify` kiểm tra hash, `--self-test` cho CI |
| `tools/SeedSqlGenerator.java` | Sinh `V2__seed_data.sql` + **tự quét** xem plaintext có lọt vào SQL không |

Xem [`docs/database/seed-mapping.md`](../database/seed-mapping.md) §6 và
[`docs/database/migration-runbook.md`](../database/migration-runbook.md) §6.

### 2.2. Xử Lý Cảnh Báo An Toàn Thông Tin (Git History)
- **Rủi ro hiện hữu:** File `data/users.txt` (chứa password plaintext) đã được commit vào lịch sử Git ở tag `v1.0-cli-final`. Do lịch sử Git là bất biến (immutable):
  - **Đổi password mẫu mới:** Đổi hoàn toàn sang bộ mật khẩu demo MỚI khi nạp vào database PostgreSQL (tuyệt đối không hash lại mật khẩu cũ đã lộ trong file text).
  - **Ghi chú bảo mật trong README:** Nêu rõ các tài khoản trong file seed data chỉ mang tính chất minh họa (demo/portfolio), khuyến cáo người dùng đổi mật khẩu hoặc không sử dụng thông tin thật.
  - **Không rewrite Git history:** Việc dùng `git filter-repo` hoặc `git rebase` để xóa file cũ khỏi lịch sử là không cần thiết với dự án cá nhân, đồng thời sẽ làm hỏng (break) tag `v1.0-cli-final` đã phát hành.

---

## 3. Chuẩn Hóa ApiResponse Wrapper

Để Frontend xử lý dữ liệu nhất quán, mọi endpoint REST API phải trả về một format thống nhất thay vì trả trực tiếp dữ liệu thô.

### 3.1. Định Nghĩa Cấu Trúc ApiResponse
Sử dụng Java `record` để định nghĩa response wrapper bất biến (immutable):

```java
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
```

### 3.2. Chuẩn Hóa Phân Trang (Pagination)
Khi danh sách phim hoặc người dùng cần phân trang thông qua query params (`page`, `size`, `sort`), trường `data` không còn là một `List<T>` đơn thuần mà phải bao bọc trong `PageResponse<T>`:

```java
public record PageResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
) {
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
```

- **Kết hợp generic lồng nhau:** `ApiResponse<PageResponse<MovieDto>>`.
- **Lợi ích:** Thiết kế generic lồng nhau ngay từ Phase 2 giúp Frontend định nghĩa interface TypeScript đồng nhất, tránh tình trạng phải refactor giao diện khi bổ sung pagination.

### 3.3. Quyết Định Kiến Trúc Về Error Response (ADR)
Khi xử lý ngoại lệ tập trung qua `@RestControllerAdvice`, có 2 hướng tiếp cận:

| Tiêu chí | Hướng 1: ApiResponse thống nhất (Khuyên dùng) | Hướng 2: RFC 7807 ProblemDetail |
|---|---|---|
| **Cấu trúc lỗi** | `ApiResponse(success=false, message="...", data=null)` | Chuẩn `ProblemDetail` (type, title, status, detail, instance) |
| **Ưu điểm** | Cực kỳ đơn giản, Frontend chỉ cần 1 interceptor duy nhất trên Axios để bắt `{ success, message }`. | Chuẩn hóa quốc tế (RFC 7807), được hỗ trợ native từ Spring Boot 3.x. |
| **Nhược điểm** | Ít metadata tiêu chuẩn về loại lỗi HTTP. | Cấu trúc khác với response thành công, Frontend phải viết 2 model xử lý. |
| **Quyết định (ADR)** | **LỰA CHỌN CHO SCOPE HIỆN TẠI:** Sử dụng Hướng 1 để giảm thiểu độ phức tạp và tăng tốc độ tích hợp giữa Frontend và Backend. Nếu sau này API mở rộng cho bên thứ ba tiêu thụ, sẽ cân nhắc chuyển dịch sang RFC 7807. |

---

## 4. API Versioning

### 4.1. Quy Ước Tiền Tố Endpoint
- Áp dụng tiền tố `/api/v1/...` ngay từ endpoint đầu tiên của dự án, **không sử dụng `/api/...` trơn**.
- Ví dụ:
  - Đăng nhập: `POST /api/v1/auth/login`
  - Danh sách phim: `GET /api/v1/movies`
  - Chi tiết phim: `GET /api/v1/movies/{id}`

### 4.2. Lý Do & Lợi Ích Lâu Dài
- **Hạn chế Breaking Changes:** Khi hệ thống nâng cấp nghiệp vụ lớn hoặc thay đổi hoàn toàn DTO response, chúng ta có thể triển khai song song `/api/v2/...` mà không làm gián đoạn phiên bản Frontend cũ đang hoạt động trên Production.
- **Dễ dàng cấu hình Gateway/Proxy:** Phân tách rõ ràng giữa các version khi cần rewrite rule trên Nginx hoặc Cloudflare.

---

## 5. Spring Profiles Tách Biệt Dev/Prod

Để đảm bảo an toàn và tính linh hoạt giữa các môi trường triển khai, dự án phân tách cấu hình thành các file profile độc lập, kích hoạt thông qua biến môi trường `SPRING_PROFILES_ACTIVE`.

### 5.1. Cấu Trúc Các File Cấu Hình
- `application.yml`: Chứa các cấu hình dùng chung (application name, base path, versioning).
- `application-dev.yml`: Môi trường phát triển cục bộ (Local Development).
- `application-prod.yml`: Môi trường triển khai thực tế (Production Cloud trên Render/Railway).

### 5.2. Bảng So Sánh Cấu Hình Chi Tiết Giữa Các Môi Trường

| Tham số | Môi trường Dev (`application-dev.yml`) | Môi trường Prod (`application-prod.yml`) |
|---|---|---|
| **Database URL** | `jdbc:postgresql://localhost:5432/cinevora_db` | Đọc từ biến `${DATABASE_URL}` |
| **DB Credentials**| `postgres` / `postgres` | Đọc từ `${DB_USERNAME}` / `${DB_PASSWORD}` |
| **Hibernate SQL** | `show-sql: true`, `format_sql: true` | `show-sql: false` (giảm tải I/O và bảo mật log) |
| **JWT Secret** | Khóa test ngắn cục bộ | Chuỗi ngẫu nhiên >= 256 bits từ `${JWT_SECRET}` |
| **Log Level** | `DEBUG` (cho package `com.cinevora`) | `INFO` hoặc `WARN` |
| **CORS Origins** | `http://localhost:5173` | `${CORS_ALLOWED_ORIGINS}` từ Cloud Dashboard |

Cấu trúc profile này là nền tảng bắt buộc để quản lý các biến môi trường cho CORS (Mục 1) và Connection Pool (Mục 6).

---

## 6. Giới Hạn Connection Pool (HikariCP) Phù Hợp Free Tier

### 6.1. Thách Thức Từ Hạ Tầng Free Tier
Các dịch vụ Cloud Database miễn phí (Render PostgreSQL, Supabase, Neon, Railway) áp dụng các chính sách tiết kiệm tài nguyên nghiêm ngặt:
- **Giới hạn số lượng connection:** Thường chỉ cho phép tối đa 10–20 concurrent connections cho toàn bộ database.
- **Cơ chế Auto-Sleep:** Một số nhà cung cấp sẽ ngắt kết nối hoặc tạm dừng instance nếu không có request trong vài phút.
- **Rủi ro cạn kiệt:** Spring Boot mặc định cấu hình HikariCP với `maximum-pool-size = 10`. Nếu ứng dụng khởi động nhiều worker hoặc không giải phóng connection kịp thời, hệ thống sẽ gặp lỗi nghiêm trọng: `FATAL: remaining connection slots are reserved for non-replication superuser connections` hoặc `Connection is not available, request timed out`.

### 6.2. Cấu Hình HikariCP Khuyến Nghị Cho Free Tier
Trong file `application-prod.yml`, cấu hình HikariCP tối ưu cho môi trường tài nguyên eo hẹp:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 3            # Giới hạn 3-5 connections, an toàn cho gói Free Tier
      minimum-idle: 1                 # Duy trì tối thiểu 1 connection sẵn sàng
      idle-timeout: 300000            # 5 phút: giải phóng connection nhàn rỗi
      max-lifetime: 1200000           # 20 phút: làm mới connection tránh bị Cloud firewall kill ngầm
      connection-timeout: 20000       # 20 giây: thời gian tối đa chờ connection trước khi timeout
      leak-detection-threshold: 30000 # 30 giây: cảnh báo nếu truy vấn giữ connection quá lâu
```

Việc tinh chỉnh pool size giúp ứng dụng hoạt động bền bỉ, không bị sập kết nối khi nhà tuyển dụng hoặc người dùng truy cập demo portfolio.

---

## 7. Checklist Tiêu Chuẩn Bảo Mật & API

- [x] Cấu hình CORS đọc từ biến môi trường `CORS_ALLOWED_ORIGINS`, sử dụng `allowedOriginPatterns` và không dùng wildcard `*` khi có credentials.
- [x] Xây dựng script hash password bằng BCrypt độc lập, đưa chuỗi hash mẫu mới vào `V2__seed_data.sql` và thêm lưu ý vào README.
- [x] Triển khai `ApiResponse<T>` wrapper thống nhất, tích hợp `PageResponse<T>` cho các endpoint phân trang và chốt phương án xử lý lỗi.
- [x] Đặt tiền tố `/api/v1/` cho toàn bộ endpoint của hệ thống ngay từ giai đoạn xây dựng Controller.
- [x] Tạo tách biệt `application-dev.yml` và `application-prod.yml`, kích hoạt linh hoạt qua `SPRING_PROFILES_ACTIVE`.
- [x] Giới hạn HikariCP `maximum-pool-size` về mức 3–5 connections trong profile production để tương thích trơn tru với PostgreSQL Free Tier.

### Bằng chứng verify — 2026-09-19

Các chuẩn trên đã được đối chiếu với source hiện tại và runtime smoke: CORS preflight từ `http://localhost:5173` trả HTTP 200 với origin hợp lệ; API dùng wrapper `ApiResponse`/`PageResponse` và prefix `/api/v1`; JWT BCrypt login, ADMIN/CUSTOMER authorization, Swagger bearerAuth, profile config và production Hikari pool đều đã được kiểm tra. BCrypt seed tiếp tục đạt parity với Spring Security và `npm.cmd audit` không phát hiện vulnerability.

Log đầy đủ xem tại [`docs/verification/MASTER_CHECK_PHASE_1_3.md`](../verification/MASTER_CHECK_PHASE_1_3.md).
