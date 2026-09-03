# PHASE 4 — Tích Hợp, Migration Dữ Liệu & Testing

**Thời gian ước tính:** 1 tuần
**Mục tiêu:** Ráp nối 2 mảnh Backend và Frontend hoạt động hoàn hảo với nhau, đưa dữ liệu mồi cũ vào DB và tiến hành chạy test toàn luồng kinh điển.

---

## 1. Migration Dữ Liệu Thực (Data Seed)

Đảm bảo dữ liệu từ phiên bản CLI không bị vứt bỏ. Quá trình Migration đòi hỏi:
- Sinh script `V2__seed_data.sql` chứa câu lệnh Insert từ hệ thống file `data/` cũ.
- Thực hiện mã hóa (Hash BCrypt) password của các accounts mẫu (bao gồm Admin) thay vì để plaintext lúc tạo script insert.
- Flyway chạy lúc startup Spring Boot phải thi hành insert thành công trên DB PostgreSQL.

---

## 2. Integration Testing & QA Luồng

Dự án cần vượt qua bài kiểm tra bằng cách đóng vai người dùng thật. Các luồng bắt buộc:

- **Luồng Auth & Content:** Đăng ký tài khoản Customer -> Tự động đăng nhập -> Mở trang danh sách phim -> Xem chi tiết 1 phim.
- **Luồng Tương tác User:** Đưa phim vào Watchlist -> Bấm xem phim (cập nhật history, progress) -> Bỏ phim vào mục Yêu thích.
- **Luồng Quản trị (Admin):** Đăng nhập bằng Account Admin -> Thêm Category mới -> Đăng 1 bộ phim thuộc Category đó -> Phim lập tức hiện lên trang chủ phía Customer -> Xem báo cáo thống kê hiển thị được view vừa rồi của Customer.

---

## 3. Cross-Check Các Rule Nghiệp Vụ Chặt Chẽ

Bản CLI đã xây dựng hệ validation rất kỹ (VD: "Không được phép xóa Category khi vẫn còn phim nằm trong nhóm"). Trong Phase này phải tiến hành cố tình tạo ra hành vi sai ở giao diện Web xem Backend có trả HTTP 400 chặn lại đúng thiết kế cũ hay không.

- Test Delete Category đang chứa Movie.
- Test Delete phim đang nằm trong Watchlist của người khác.
- Đảm bảo chức năng Undo/Redo (nếu được triển khai trên React hoặc Redis) hoạt động không bị crash.

---

## 4. Checklist Hoàn Thành Phase 4

- [ ] Data migration hoàn tất, bảng PostgreSQL có đủ dữ liệu hạt giống (users, categories, movies,...).
- [ ] Vượt qua toàn bộ kịch bản test luồng người dùng (Integration End-to-End).
- [ ] Cross-check thành công 100% các validation business logic khắt khe từ CLI.
- [ ] Test hiệu năng cơ bản (tốc độ query/filter phim, search).
- [ ] Check bảo mật JWT expired và kiểm tra phân quyền tài nguyên theo Role.
