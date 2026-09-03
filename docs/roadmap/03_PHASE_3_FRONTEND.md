# PHASE 3 — Xây Dựng Frontend (React)

**Thời gian ước tính:** 2–3 tuần
**Mục tiêu:** Xây dựng một Single Page Application (SPA) trên nền React kết hợp TypeScript, thay thế hoàn toàn trải nghiệm giao diện CLI Menu bằng đồ họa.

---

## 1. Khởi Tạo Dự Án

Sử dụng Vite để boot frontend cho tốc độ cực cao:
```bash
npm create vite@latest frontend -- --template react-ts
```

**Các thư viện chủ đạo được chọn:**
- **React Router v6**: Điều hướng trang client-side.
- **TanStack Query (React Query)**: Xử lý fetching dữ liệu REST, auto-caching, quản lý loading/error state.
- **Axios**: HTTP Client cấu hình sẵn base URL và interceptors nhét JWT Header.
- **Zustand**: Quản lý state nhẹ nhàng cho Auth session (người dùng đang đăng nhập, lưu token).
- **TailwindCSS**: Utilities class giúp styling giao diện web mượt mà, hỗ trợ responsive nhanh chóng.
- **React Hook Form + Zod**: Quản lý form data, kết hợp validate schema trực tiếp phía browser trước khi gọi API.

---

## 2. Bản Đồ Trang Giao Diện (Pages)

Mỗi Menu/View bên bản CLI cũ sẽ được ánh xạ thành 1 React Page:

| Trang UI (React Component) | Tương ứng tính năng ở hệ thống CLI | Mô tả |
|---|---|---|
| `LoginPage`, `RegisterPage` | `LoginView` | Giao diện đăng nhập/đăng ký |
| `AdminDashboardPage` | `AdminView` menu chính | Bảng điều khiển quản trị viên |
| `AdminCategoryPage` | `AdminView` -> Category | Quản lý, xóa, restore Category |
| `AdminMoviePage` | `AdminView` -> Movie | Quản lý Phim, thêm sửa nội dung |
| `AdminStatisticsPage`| Tính năng Báo Cáo/Trending | Hiển thị Chart, Bảng xếp hạng phim |
| `BrowseMoviePage` | `CustomerView.browseMovies()` | Danh sách phim cho user duyệt |
| `MovieDetailPage` | `showMovieDetail()` | Thông tin 1 phim + Button thêm watch |
| `SearchPage` | `advancedFilterMenu` | Tìm kiếm phim với nhiều tiêu chí (Genre, Title) |
| `WatchlistPage` | Menu Watchlist | Thư viện lưu phim xem sau |
| `FavouritesPage` | Menu Favourites | Thư viện phim yêu thích |
| `ContinueWatchingPage`| Resume tiến trình phim | Nơi hiển thị các phim đang xem dở |

---

## 3. Checklist Hoàn Thành Phase 3

- [ ] Thiết kế và dựng thành công Layout vỏ ngoài (Navbar, Sidebar, Footer).
- [ ] Thiết lập React Router cơ bản và các `ProtectedRoute` để chặn người chưa đăng nhập.
- [ ] Hoàn thành luồng Authentication thực tế: Giao diện Login gọi API Backend -> nhận Token -> Lưu trạng thái vào Zustand.
- [ ] Hoàn thiện các luồng duyệt nội dung, tìm kiếm, filter dành cho Customer.
- [ ] Hoàn thiện Dashboard và các trang Form CRUD (Thêm, sửa, xóa) phục vụ phân hệ Admin.
- [ ] Hoàn thiện các trang tiện ích người dùng (Watchlist, History).
- [ ] Tối ưu hiển thị Responsive cho màn hình Điện Thoại (Mobile) và Máy tính (Desktop).
- [ ] Tinh chỉnh Loading spinner và xử lý Error (Toast Notification) nhất quán trên toàn App.
