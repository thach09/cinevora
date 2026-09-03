# PHASE 5 — DevOps & Deployment

**Thời gian ước tính:** 1 tuần
**Mục tiêu:** Đóng gói sản phẩm chuẩn Containerization, cấu hình luồng CI/CD tự động và triển khai hệ thống lên Cloud phục vụ truy cập qua public Internet.

---

## 1. Containerization (Đóng Gói Ứng Dụng)

Đóng gói các module thành Docker Image để chạy mượt mà không phân biệt hệ điều hành Host:
- **Backend `Dockerfile`**: Dùng Multi-stage build (Stage 1: Maven pull lib và build thành `.jar` -> Stage 2: Nhúng vào image chứa JRE runtime để nhẹ file).
- **Frontend `Dockerfile`**: Tương tự, build ra gói static HTML/JS và dùng Nginx để serve, cấu hình điều hướng về `index.html` của React SPA.
- Tạo file `docker-compose.yml` ở thư mục gốc giúp developer khác pull repo chỉ cần gõ 1 lệnh là chạy cả `backend` + `frontend` + `postgresql`.

---

## 2. Cấu Hình CI/CD Pipeline (GitHub Actions)

Tạo các workflows để làm tự động:

- **Pipeline `.github/workflows/backend-ci.yml`**: Lắng nghe commit vào folder `backend/`. Chạy unit test (`mvn test`).
- **Pipeline `.github/workflows/frontend-ci.yml`**: Lắng nghe commit vào folder `frontend/`. Chạy linter, check TypeScript và build test (`npm run build`).
- **Pipeline Deploy**: Khi code merge vào nhánh `main`, tự động trigger webhook để push phiên bản mới lên các server Cloud.

---

## 3. Lựa Chọn Nền Tảng Deploy (Portfolio Oriented)

Hệ thống sẽ được triển khai chia cắt theo mô hình Serverless/PaaS để tiết kiệm tài nguyên và dễ cài đặt:

- **Database (PostgreSQL)**: Sử dụng các nhà cung cấp như *Render*, *Supabase*, hoặc *Neon* để lấy Free-tier RDBMS.
- **Backend (Spring Boot App)**: Đưa container lên *Render Web Service* hoặc *Railway.app*.
- **Frontend (Static SPA)**: Kết nối kho Git trực tiếp với *Vercel* hoặc *Netlify*. Các nền tảng này cung cấp CDN toàn cầu cực nhanh cho frontend tĩnh.
- **Tên miền (Domain) - Tùy chọn**: Có thể kết nối thêm domain tùy chỉnh bằng Cloudflare cho ứng dụng thật sự chuyên nghiệp.

---

## 4. Checklist Hoàn Thành Phase 5

- [ ] Viết thành công `Dockerfile` có thể build ra image hoạt động được cho cả backend và frontend.
- [ ] Lệnh `docker-compose up` tại local khởi chạy hoàn chỉnh toàn bộ ứng dụng thành 1 khối.
- [ ] Các badges của GitHub Actions báo xanh (Test Passed) trên kho lưu trữ.
- [ ] Database và Backend đã chạy public trên Render/Railway, có thể cURL hoặc gọi qua Swagger URL trên mạng.
- [ ] Frontend đã live trên Vercel/Netlify, config biến môi trường trỏ đúng vào URL Backend.
- [ ] Xử lý dứt điểm các lỗi liên quan đến bảo mật trình duyệt, cụ thể là **CORS** giữa URL của Frontend và Backend.
- [ ] Ứng dụng chạy End-To-End hoàn hảo trên Internet.
