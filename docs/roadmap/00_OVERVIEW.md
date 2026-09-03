# CINEVORA — Tóm Tắt Tổng Quan (Executive Summary)

Tài liệu này ghi nhận lại toàn bộ kế hoạch chuyển đổi dự án Cinevora từ kiến trúc CLI (Command Line Interface) sang một Full-Stack Web Application hiện đại. 

Dự án bản CLI đã hoàn thiện cực kỳ tốt nền tảng OOP cốt lõi, bao gồm đầy đủ 4 tính chất OOP, triển khai các thuật toán (Bubble Sort, Linear Search, Custom Stack cho Undo/Redo) và validate 3 lớp chặt chẽ. Nền tảng này cho phép chúng ta tái sử dụng phần lớn Business Logic.

Mục tiêu của kế hoạch là tạo ra một dự án Web hoàn chỉnh, public được trên Internet, đáp ứng mục đích làm portfolio cá nhân và thực hành quy trình phát triển phần mềm chuẩn công nghiệp.

---

## 1. Quyết Định Công Nghệ Đã Chốt (Tech Stack)

| Thành phần | Công nghệ | Lý do chọn |
|---|---|---|
| **Backend** | **Spring Boot 3.x (Java 21 LTS)** | Tận dụng lại 90% kiến thức OOP và code logic từ dự án cũ; hệ sinh thái Java Backend mạnh mẽ. |
| **Database** | **PostgreSQL 16** | RDBMS mã nguồn mở mạnh mẽ, dễ quản lý schema và miễn phí trên nhiều Cloud. |
| **ORM** | **Spring Data JPA + Hibernate** | Ánh xạ Entity tự nhiên, giảm boilerplate code. |
| **Frontend** | **React 18 + Vite + TypeScript** | Chuẩn công nghiệp hiện tại, hệ sinh thái thư viện phong phú. |
| **API Style** | **REST + OpenAPI 3.0 (Swagger)** | Giao tiếp chuẩn mực, có công cụ gen docs tự động. |
| **Auth** | **Spring Security + JWT** | Xác thực stateless, cực kỳ phù hợp cho ứng dụng REST. |
| **Containerization**| **Docker + Docker Compose** | Đồng nhất môi trường từ Dev đến Production. |
| **CI/CD** | **GitHub Actions** | Tích hợp sẵn với repo, miễn phí mạnh mẽ. |
| **Hosting** | **Render/Railway (BE/DB)** & **Vercel/Netlify (FE)** | Có free tier, tốc độ deploy nhanh chóng. |
| **Schema DB** | **Flyway** | Quản lý version database, dễ dàng migrate giữa các môi trường. |

---

## 2. Chiến Lược Tái Sử Dụng Code (Reuse Mapping)

*Nguyên tắc cốt lõi: Business Logic là bất biến theo giao diện. Chỉ tầng Presentation (View) và tầng Persistence (Repository implement) thay đổi.*

- Thư mục `model/`: **Giữ lại ~95%**. Bổ sung thêm các annotation JPA (e.g. `@Entity`, `@Id`, `@ManyToOne`) và di chuyển sang package `entity/`.
- Thư mục `controller/`: **Giữ lại ~90% logic**. Đổi tên thành các class `*Service` và thay đổi lời gọi lưu trữ file sang gọi Spring Data JPA.
- Thư mục `repository/`: **Giữ lại các interface chữ ký**. Chuyển sang kế thừa `JpaRepository<T, ID>`.
- Thư mục `utils/`: **Giữ lại 100%**. Code tiện ích như `SearchUtils`, `SortUtils`, logic `CustomStack` hay `ReportExporter` có thể tái sử dụng trực tiếp hoặc tham khảo thuật toán mang sang JS/TS.
- Thư mục `view/`, `Main.java`, Validation Console: **Xóa hoàn toàn**. Sẽ được thay thế bởi REST API, React Components và Bean Validation.
- Dữ liệu `data/*.txt`: Dùng làm seed data thông qua file migration SQL để nạp vào hệ thống mới.

---

## 3. Kiến Trúc Hệ Thống Đích

Dự án sẽ chuyển đổi sang mô hình **Monorepo** với sơ đồ tương tác sau:

1. **Client (Browser)**: React SPA giao tiếp qua HTTPS bằng Axios.
2. **Backend**: Spring Boot nhận Request -> Controller (REST) -> Service (Business Logic) -> Repository (JPA).
3. **Database**: PostgreSQL kết nối qua JDBC (Hibernate).

Cấu trúc cây thư mục định hướng:
```text
cinevora/
├── legacy-cli/          # Lưu trữ code CLI cũ (không deploy, chỉ để tham khảo)
├── backend/             # Dự án Spring Boot Java 21
├── frontend/            # Dự án React TypeScript
├── docs/                # Chứa tài liệu thiết kế dự án (Roadmap, ERD, API Specs)
├── docker-compose.yml   # Chạy toàn bộ database/services ở local
└── .github/workflows/   # CI/CD config
```

---

## 4. Timeline & Roadmap Chi Tiết

Tổng thời gian thực hiện ước tính là **7–9 tuần** làm part-time.

- **PHASE 0:** Cleanup & Chuẩn bị Repo ✅ *(Đã hoàn thành)*
- **[PHASE 1 (1 tuần): Thiết Kế Database](./01_PHASE_1_DATABASE.md)**
- **[PHASE 2 (2-3 tuần): Xây Dựng Backend (Spring Boot)](./02_PHASE_2_BACKEND.md)**
- **[PHASE 3 (2-3 tuần): Xây Dựng Frontend (React)](./03_PHASE_3_FRONTEND.md)**
- **[PHASE 4 (1 tuần): Tích Hợp & Testing](./04_PHASE_4_INTEGRATION_TESTING.md)**
- **[PHASE 5 (1 tuần): DevOps & Deployment](./05_PHASE_5_DEVOPS_DEPLOYMENT.md)**
- **[Security & API Standards (Cross-cutting)](./06_SECURITY_AND_API_STANDARDS.md)**

---

## 5. Rủi Ro Cần Lưu Ý

1. **Bảo mật mật khẩu:** Code cũ lưu plaintext. Bắt buộc phải hash bằng `BCrypt` trước khi lưu database.
2. **Circular Dependency:** Injection chéo (như Category và Movie) cần được xử lý đúng chuẩn Spring DI (`@Lazy` hoặc refactor service).
3. **State Undo/Redo:** RAM-based trên CLI sẽ khó duy trì trên web stateless. Sẽ cần đưa logic sang state của React trên Frontend hoặc lưu tạm Redis.

Xem chi tiết cách xử lý các rủi ro này tại 06_SECURITY_AND_API_STANDARDS.md.

