-- =====================================================================
-- CINEVORA - Migration V2 : Seed data (du lieu mau tu ban CLI cu)
-- =====================================================================
-- FILE NAY DUOC SINH TU DONG - KHONG SUA TAY.
--   Cong cu sinh : tools/SeedSqlGenerator.java (chi can JDK, khong can Maven)
--   Nguon du lieu: legacy-cli/data/{categories,movies,users}.txt
--   Tai lieu     : docs/database/seed-mapping.md
--
-- BANG ANH XA ID (xac dinh, 1-1):
--   CATnn (categories.txt) -> n
--   Mnn   (movies.txt)     -> n
--   users.txt              -> so thu tu dong, 1-based
--
-- BAO MAT: cot users.password KHONG lay tu CLI (CLI luu plaintext).
-- Tat ca tai khoan dung chung 1 BCrypt hash (cost 10) cua mat khau demo.
-- Mat khau demo nam trong .env.example va seed-mapping.md, KHONG ghi o day.
--
-- THOI DIEM: gia lap, xac dinh. Moc goc 2026-01-01 00:00:00+00:00
-- 7 bang lan luot lech nhau 1 ngay (categories = moc goc +1 ngay,
-- movies = +2, users = +3, ... continue_watching = +7); trong moi bang
-- cac dong lech nhau 17 phut. updated_at = created_at.
--
-- QUY MO SEED:
--   categories          = 7
--   movies              = 60
--   users               = 11
--   watchlist           = 7
--   favourites          = 4
--   watch_history       = 9
--   continue_watching   = 6
--   TONG                = 104 dong
-- =====================================================================

INSERT INTO categories (id, name, description, is_active, created_at, updated_at) VALUES
    (1, 'Hành Động', 'Phim hành động, cháy nổ, rượt đuổi', true, '2026-01-02 00:00:00+00:00', '2026-01-02 00:00:00+00:00'),
    (2, 'Tình Cảm', 'Phim tình cảm lãng mạn', true, '2026-01-02 00:17:00+00:00', '2026-01-02 00:17:00+00:00'),
    (3, 'Kinh Dị', 'Phim kinh dị, rùng rợn', true, '2026-01-02 00:34:00+00:00', '2026-01-02 00:34:00+00:00'),
    (4, 'Hài Hước', 'Phim hài, giải trí nhẹ nhàng', true, '2026-01-02 00:51:00+00:00', '2026-01-02 00:51:00+00:00'),
    (5, 'Khoa Học Viễn Tưởng', 'Phim sci-fi, vũ trụ, công nghệ', true, '2026-01-02 01:08:00+00:00', '2026-01-02 01:08:00+00:00'),
    (6, 'Hoạt hình', 'Phim hoạt hình, giải trí', true, '2026-01-02 01:25:00+00:00', '2026-01-02 01:25:00+00:00'),
    (7, 'Tài liệu', 'Phim thực tế, lịch sử, đời sống', true, '2026-01-02 01:42:00+00:00', '2026-01-02 01:42:00+00:00');

INSERT INTO movies (id, category_id, title, director, actors, release_year, rating, views, favourites_count, is_active, created_at, updated_at) VALUES
    (1, 1, 'Avengers: Endgame', 'Anthony Russo', 'Robert Downey Jr., Chris Evans', 2019, 8.4, 279000002, 15000001, true, '2026-01-03 00:00:00+00:00', '2026-01-03 00:00:00+00:00'),
    (2, 1, 'Avengers: Infinity War', 'Anthony Russo', 'Robert Downey Jr., Chris Hemsworth', 2018, 8.4, 204000000, 12000001, true, '2026-01-03 00:17:00+00:00', '2026-01-03 00:17:00+00:00'),
    (3, 1, 'Spider-Man: No Way Home', 'Jon Watts', 'Tom Holland, Zendaya', 2021, 8.2, 192000000, 11000001, true, '2026-01-03 00:34:00+00:00', '2026-01-03 00:34:00+00:00'),
    (4, 1, 'Fast & Furious 7', 'James Wan', 'Vin Diesel, Paul Walker', 2015, 7.1, 151000000, 8000000, true, '2026-01-03 00:51:00+00:00', '2026-01-03 00:51:00+00:00'),
    (5, 1, 'Hai Phượng', 'Lê Văn Kiệt', 'Ngô Thanh Vân, Phan Thanh Nhiên', 2019, 8.0, 25000000, 1500000, true, '2026-01-03 01:08:00+00:00', '2026-01-03 01:08:00+00:00'),
    (6, 1, 'Lật Mặt 6: Tấm Vé Định Mệnh', 'Lý Hải', 'Quốc Cường, Trung Dũng', 2023, 8.2, 35000000, 2100000, true, '2026-01-03 01:25:00+00:00', '2026-01-03 01:25:00+00:00'),
    (7, 1, 'Thanh Sói: Cúc Dại Trong Đêm', 'Ngô Thanh Vân', 'Đồng Ánh Quỳnh, Tóc Tiên', 2022, 7.5, 12000000, 850000, true, '2026-01-03 01:42:00+00:00', '2026-01-03 01:42:00+00:00'),
    (8, 1, 'John Wick 4', 'Chad Stahelski', 'Keanu Reeves, Donnie Yen', 2023, 8.2, 42000001, 2800000, true, '2026-01-03 01:59:00+00:00', '2026-01-03 01:59:00+00:00'),
    (9, 1, 'Bụi Đời Chợ Lớn', 'Charlie Nguyễn', 'Johnny Trí Nguyễn, Hoàng Phúc', 2013, 7.9, 18000000, 950000, true, '2026-01-03 02:16:00+00:00', '2026-01-03 02:16:00+00:00'),
    (10, 1, 'Captain America: Civil War', 'Anthony Russo', 'Chris Evans, Robert Downey Jr.', 2016, 7.8, 115000000, 6500000, true, '2026-01-03 02:33:00+00:00', '2026-01-03 02:33:00+00:00'),
    (11, 2, 'Mai', 'Trấn Thành', 'Phương Anh Đào, Tuấn Trần', 2024, 8.5, 65000000, 4800000, true, '2026-01-03 02:50:00+00:00', '2026-01-03 02:50:00+00:00'),
    (12, 2, 'Mắt Biếc', 'Victor Vũ', 'Trần Nghĩa, Trúc Anh', 2019, 8.3, 45000000, 3200000, true, '2026-01-03 03:07:00+00:00', '2026-01-03 03:07:00+00:00'),
    (13, 2, 'Nhà Bà Nữ', 'Trấn Thành', 'Lê Giang, Uyển Ân', 2023, 8.1, 58000000, 4200000, true, '2026-01-03 03:24:00+00:00', '2026-01-03 03:24:00+00:00'),
    (14, 2, 'Titanic', 'James Cameron', 'Leonardo DiCaprio, Kate Winslet', 1997, 8.9, 220000000, 18000000, true, '2026-01-03 03:41:00+00:00', '2026-01-03 03:41:00+00:00'),
    (15, 2, 'La La Land', 'Damien Chazelle', 'Ryan Gosling, Emma Stone', 2016, 8.0, 38000000, 2500000, true, '2026-01-03 03:58:00+00:00', '2026-01-03 03:58:00+00:00'),
    (16, 2, 'The Notebook', 'Nick Cassavetes', 'Ryan Gosling, Rachel McAdams', 2004, 7.8, 25000000, 1800000, true, '2026-01-03 04:15:00+00:00', '2026-01-03 04:15:00+00:00'),
    (17, 2, 'Cua Lại Vợ Bầu', 'Nhất Trung', 'Trấn Thành, Ninh Dương Lan Ngọc', 2019, 7.5, 35000000, 2000000, true, '2026-01-03 04:32:00+00:00', '2026-01-03 04:32:00+00:00'),
    (18, 2, 'Tháng Năm Rực Rỡ', 'Nguyễn Quang Dũng', 'Hoàng Yến Chibi, Jun Vũ', 2018, 8.0, 22000000, 1500000, true, '2026-01-03 04:49:00+00:00', '2026-01-03 04:49:00+00:00'),
    (19, 2, 'Your Name', 'Makoto Shinkai', 'Ryunosuke Kamiki, Mone Kamishiraishi', 2016, 8.4, 45000000, 3100000, true, '2026-01-03 05:06:00+00:00', '2026-01-03 05:06:00+00:00'),
    (20, 2, 'Me Before You', 'Thea Sharrock', 'Emilia Clarke, Sam Claflin', 2016, 7.4, 21000000, 1400000, true, '2026-01-03 05:23:00+00:00', '2026-01-03 05:23:00+00:00'),
    (21, 3, 'Quả Tim Máu', 'Victor Vũ', 'Thái Hòa, Nhã Phương', 2014, 7.6, 15000000, 950000, true, '2026-01-03 05:40:00+00:00', '2026-01-03 05:40:00+00:00'),
    (22, 3, 'Kẻ Ăn Hồn', 'Trần Hữu Tấn', 'Hoàng Hà, Võ Điền Gia Huy', 2023, 7.8, 28000000, 1800000, true, '2026-01-03 05:57:00+00:00', '2026-01-03 05:57:00+00:00'),
    (23, 3, 'Bóng Đè', 'Lê Văn Kiệt', 'Quang Tuấn, Lâm Thanh Mỹ', 2022, 6.5, 12000000, 650000, true, '2026-01-03 06:14:00+00:00', '2026-01-03 06:14:00+00:00'),
    (24, 3, 'Thất Sơn Tâm Linh', 'Hàm Trần', 'Hoàng Yến Chibi, Quang Tuấn', 2019, 6.8, 18000000, 1100000, true, '2026-01-03 06:31:00+00:00', '2026-01-03 06:31:00+00:00'),
    (25, 3, 'The Conjuring', 'James Wan', 'Vera Farmiga, Patrick Wilson', 2013, 7.5, 35000000, 2200000, true, '2026-01-03 06:48:00+00:00', '2026-01-03 06:48:00+00:00'),
    (26, 3, 'IT', 'Andy Muschietti', 'Bill Skarsgård, Jaeden Martell', 2017, 7.3, 28000001, 1700000, true, '2026-01-03 07:05:00+00:00', '2026-01-03 07:05:00+00:00'),
    (27, 3, 'Annabelle', 'John R. Leonetti', 'Annabelle Wallis, Ward Horton', 2014, 5.4, 25000000, 1200000, true, '2026-01-03 07:22:00+00:00', '2026-01-03 07:22:00+00:00'),
    (28, 3, 'The Nun', 'Corin Hardy', 'Demián Bichir, Taissa Farmiga', 2018, 5.3, 33000000, 1500000, true, '2026-01-03 07:39:00+00:00', '2026-01-03 07:39:00+00:00'),
    (29, 3, 'Get Out', 'Jordan Peele', 'Daniel Kaluuya, Allison Williams', 2017, 7.7, 24000000, 1500000, true, '2026-01-03 07:56:00+00:00', '2026-01-03 07:56:00+00:00'),
    (30, 3, 'A Quiet Place', 'John Krasinski', 'Emily Blunt, John Krasinski', 2018, 7.5, 26000000, 1600000, true, '2026-01-03 08:13:00+00:00', '2026-01-03 08:13:00+00:00'),
    (31, 4, 'Em Chưa 18', 'Lê Thanh Sơn', 'Kaity Nguyễn, Kiều Minh Tuấn', 2017, 8.0, 35000000, 2500000, true, '2026-01-03 08:30:00+00:00', '2026-01-03 08:30:00+00:00'),
    (32, 4, 'Tèo Em', 'Charlie Nguyễn', 'Thái Hòa, Johnny Trí Nguyễn', 2013, 7.5, 21000000, 1300000, true, '2026-01-03 08:47:00+00:00', '2026-01-03 08:47:00+00:00'),
    (33, 4, 'Chàng Vợ Của Em', 'Charlie Nguyễn', 'Thái Hòa, Phương Anh Đào', 2018, 7.7, 19000000, 1200000, true, '2026-01-03 09:04:00+00:00', '2026-01-03 09:04:00+00:00'),
    (34, 4, 'Gái Già Lắm Chiêu 3', 'Bảo Nhân', 'Ninh Dương Lan Ngọc, Lê Khanh', 2020, 7.2, 15000000, 850000, true, '2026-01-03 09:21:00+00:00', '2026-01-03 09:21:00+00:00'),
    (35, 4, 'Siêu Lừa Gặp Siêu Lầy', 'Võ Thanh Hòa', 'Anh Tú, Mạc Văn Khoa', 2023, 7.4, 28000000, 1600001, true, '2026-01-03 09:38:00+00:00', '2026-01-03 09:38:00+00:00'),
    (36, 4, 'Home Alone', 'Chris Columbus', 'Macaulay Culkin, Joe Pesci', 1990, 7.7, 45000001, 2800000, true, '2026-01-03 09:55:00+00:00', '2026-01-03 09:55:00+00:00'),
    (37, 4, 'Mr. Bean''s Holiday', 'Steve Bendelack', 'Rowan Atkinson, Emma de Caunes', 2007, 6.4, 32000000, 1900000, true, '2026-01-03 10:12:00+00:00', '2026-01-03 10:12:00+00:00'),
    (38, 4, 'The Hangover', 'Todd Phillips', 'Bradley Cooper, Ed Helms', 2009, 7.7, 32000000, 1900000, true, '2026-01-03 10:29:00+00:00', '2026-01-03 10:29:00+00:00'),
    (39, 4, 'Kung Fu Hustle', 'Stephen Chow', 'Stephen Chow, Yuen Wah', 2004, 7.7, 40000000, 2500000, true, '2026-01-03 10:46:00+00:00', '2026-01-03 10:46:00+00:00'),
    (40, 4, 'Minions', 'Kyle Balda', 'Sandra Bullock, Jon Hamm', 2015, 6.4, 50000000, 3000000, true, '2026-01-03 11:03:00+00:00', '2026-01-03 11:03:00+00:00'),
    (41, 5, 'Avatar', 'James Cameron', 'Sam Worthington, Zoe Saldana', 2009, 7.9, 292000000, 16000000, true, '2026-01-03 11:20:00+00:00', '2026-01-03 11:20:00+00:00'),
    (42, 5, 'Avatar: The Way of Water', 'James Cameron', 'Sam Worthington, Zoe Saldana', 2022, 7.6, 232000000, 12000000, true, '2026-01-03 11:37:00+00:00', '2026-01-03 11:37:00+00:00'),
    (43, 5, 'Maika: Cô Bé Đến Từ Hành Tinh Khác', 'Hàm Trần', 'Chu Diệp Anh, Lại Trường Phú', 2022, 7.0, 8500001, 550000, true, '2026-01-03 11:54:00+00:00', '2026-01-03 11:54:00+00:00'),
    (44, 5, 'Interstellar', 'Christopher Nolan', 'Matthew McConaughey, Anne Hathaway', 2014, 8.7, 55000000, 3800000, true, '2026-01-03 12:11:00+00:00', '2026-01-03 12:11:00+00:00'),
    (45, 5, 'Inception', 'Christopher Nolan', 'Leonardo DiCaprio, Joseph Gordon-Levitt', 2010, 8.8, 62000000, 4200000, true, '2026-01-03 12:28:00+00:00', '2026-01-03 12:28:00+00:00'),
    (46, 5, 'The Matrix', 'Lana Wachowski', 'Keanu Reeves, Laurence Fishburne', 1999, 8.7, 48000001, 3200000, true, '2026-01-03 12:45:00+00:00', '2026-01-03 12:45:00+00:00'),
    (47, 5, 'Guardians of the Galaxy', 'James Gunn', 'Chris Pratt, Zoe Saldana', 2014, 8.0, 77000000, 4500000, true, '2026-01-03 13:02:00+00:00', '2026-01-03 13:02:00+00:00'),
    (48, 5, 'Jurassic World', 'Colin Trevorrow', 'Chris Pratt, Bryce Dallas Howard', 2015, 6.9, 167000001, 9000000, true, '2026-01-03 13:19:00+00:00', '2026-01-03 13:19:00+00:00'),
    (49, 5, 'Transformers: Dark of the Moon', 'Michael Bay', 'Shia LaBeouf, Rosie Huntington-Whiteley', 2011, 6.2, 112000001, 6000000, true, '2026-01-03 13:36:00+00:00', '2026-01-03 13:36:00+00:00'),
    (50, 5, 'Dune: Part One', 'Denis Villeneuve', 'Timothée Chalamet, Rebecca Ferguson', 2021, 8.0, 40000000, 2500000, true, '2026-01-03 13:53:00+00:00', '2026-01-03 13:53:00+00:00'),
    (51, 6, 'Doraemon: Nobita và Bản Giao Hưởng Địa Cầu', 'Kazuaki Imai', 'Wasabi Mizuta, Megumi Ohara', 2024, 8.2, 45000000, 2500000, true, '2026-01-03 14:10:00+00:00', '2026-01-03 14:10:00+00:00'),
    (52, 6, 'Thám Tử Lừng Danh Conan: Ngôi Sao 5 Cánh 1 Triệu Đô', 'Chika Nagaoka', 'Minami Takayama, Wakana Yamazaki', 2024, 8.5, 60000000, 3500000, true, '2026-01-03 14:27:00+00:00', '2026-01-03 14:27:00+00:00'),
    (53, 6, 'Inside Out 2', 'Kelsey Mann', 'Amy Poehler, Maya Hawke', 2024, 8.8, 185000000, 12000000, true, '2026-01-03 14:44:00+00:00', '2026-01-03 14:44:00+00:00'),
    (54, 6, 'Frozen', 'Chris Buck', 'Kristen Bell, Idina Menzel', 2013, 7.4, 150000000, 9500000, true, '2026-01-03 15:01:00+00:00', '2026-01-03 15:01:00+00:00'),
    (55, 6, 'Minions: Sự Trỗi Dậy Của Gru', 'Kyle Balda', 'Steve Carell, Pierre Coffin', 2022, 6.5, 85000000, 5500000, true, '2026-01-03 15:18:00+00:00', '2026-01-03 15:18:00+00:00'),
    (56, 6, 'Spirited Away', 'Hayao Miyazaki', 'Rumi Hiiragi, Miyu Irino', 2001, 8.6, 60000000, 4500000, true, '2026-01-03 15:35:00+00:00', '2026-01-03 15:35:00+00:00'),
    (57, 6, 'Zootopia', 'Byron Howard', 'Ginnifer Goodwin, Jason Bateman', 2016, 8.0, 95000000, 6500000, true, '2026-01-03 15:52:00+00:00', '2026-01-03 15:52:00+00:00'),
    (58, 6, 'Toy Story 4', 'Josh Cooley', 'Tom Hanks, Tim Allen', 2019, 7.7, 85000000, 5800000, true, '2026-01-03 16:09:00+00:00', '2026-01-03 16:09:00+00:00'),
    (59, 6, 'The Lion King', 'Roger Allers', 'Matthew Broderick, Jeremy Irons', 1994, 8.5, 110000000, 8500000, true, '2026-01-03 16:26:00+00:00', '2026-01-03 16:26:00+00:00'),
    (60, 6, 'Kung Fu Panda 4', 'Mike Mitchell', 'Jack Black, Awkwafina', 2024, 7.5, 55000000, 3800000, true, '2026-01-03 16:43:00+00:00', '2026-01-03 16:43:00+00:00');

INSERT INTO users (id, username, email, password, full_name, role, is_active, created_at, updated_at) VALUES
    (1, 'admin', 'admin@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'System Admin', 'ADMIN', true, '2026-01-04 00:00:00+00:00', '2026-01-04 00:00:00+00:00'),
    (2, 'thietthach09', 'thietthachdo@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Đỗ Thiết Thạch', 'CUSTOMER', true, '2026-01-04 00:17:00+00:00', '2026-01-04 00:17:00+00:00'),
    (3, 'cristiano07', 'cr7@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Cristiano Ronaldo', 'CUSTOMER', true, '2026-01-04 00:34:00+00:00', '2026-01-04 00:34:00+00:00'),
    (4, 'messi10', 'messi10@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Lionel Messi', 'CUSTOMER', true, '2026-01-04 00:51:00+00:00', '2026-01-04 00:51:00+00:00'),
    (5, 'sontungmtp', 'mtp@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Nguyễn Thanh Tùng', 'CUSTOMER', true, '2026-01-04 01:08:00+00:00', '2026-01-04 01:08:00+00:00'),
    (6, 'jack97', 'jack97@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Trịnh Trần Phương Tuấn', 'CUSTOMER', true, '2026-01-04 01:25:00+00:00', '2026-01-04 01:25:00+00:00'),
    (7, 'neymarjr', 'neymarjr@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Neymar Junior', 'CUSTOMER', true, '2026-01-04 01:42:00+00:00', '2026-01-04 01:42:00+00:00'),
    (8, 'charlieputh', 'charlieputh@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Charlie Puth', 'CUSTOMER', true, '2026-01-04 01:59:00+00:00', '2026-01-04 01:59:00+00:00'),
    (9, 'tomholland', 'tomholland@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Tom Holland', 'CUSTOMER', true, '2026-01-04 02:16:00+00:00', '2026-01-04 02:16:00+00:00'),
    (10, 'taylorswift13', 'taylorswift13@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Taylor Swift', 'CUSTOMER', true, '2026-01-04 02:33:00+00:00', '2026-01-04 02:33:00+00:00'),
    (11, 'hieut2', 'hieuthuhai@gmail.com', '$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6', 'Trần Minh Hiếu', 'CUSTOMER', true, '2026-01-04 02:50:00+00:00', '2026-01-04 02:50:00+00:00');

INSERT INTO watchlist (user_id, movie_id, added_at) VALUES
    (2, 27, '2026-01-05 00:00:00+00:00'),
    (2, 33, '2026-01-05 00:17:00+00:00'),
    (4, 1, '2026-01-05 00:34:00+00:00'),
    (4, 60, '2026-01-05 00:51:00+00:00'),
    (4, 43, '2026-01-05 01:08:00+00:00'),
    (4, 33, '2026-01-05 01:25:00+00:00'),
    (4, 36, '2026-01-05 01:42:00+00:00');

INSERT INTO favourites (user_id, movie_id, added_at) VALUES
    (2, 3, '2026-01-06 00:00:00+00:00'),
    (2, 1, '2026-01-06 00:17:00+00:00'),
    (2, 2, '2026-01-06 00:34:00+00:00'),
    (4, 35, '2026-01-06 00:51:00+00:00');

INSERT INTO watch_history (user_id, movie_id, watched_at) VALUES
    (2, 48, '2026-01-07 00:00:00+00:00'),
    (2, 36, '2026-01-07 00:17:00+00:00'),
    (2, 8, '2026-01-07 00:34:00+00:00'),
    (2, 49, '2026-01-07 00:51:00+00:00'),
    (4, 46, '2026-01-07 01:08:00+00:00'),
    (4, 1, '2026-01-07 01:25:00+00:00'),
    (4, 1, '2026-01-07 01:42:00+00:00'),
    (4, 43, '2026-01-07 01:59:00+00:00'),
    (4, 26, '2026-01-07 02:16:00+00:00');

INSERT INTO continue_watching (user_id, movie_id, percent, updated_at) VALUES
    (2, 36, 45, '2026-01-08 00:00:00+00:00'),
    (2, 8, 0, '2026-01-08 00:17:00+00:00'),
    (2, 48, 33, '2026-01-08 00:34:00+00:00'),
    (2, 49, 0, '2026-01-08 00:51:00+00:00'),
    (4, 46, 68, '2026-01-08 01:08:00+00:00'),
    (4, 1, 79, '2026-01-08 01:25:00+00:00');

-- =====================================================================
-- DONG BO IDENTITY SEQUENCE (BAT BUOC)
-- ---------------------------------------------------------------------
-- V1 khai bao id la GENERATED BY DEFAULT AS IDENTITY de V2 co the chen id
-- tuong minh 1..n. PostgreSQL KHONG tu tang sequence khi id duoc chen thu
-- cong, nen sequence van o 1 -> lan INSERT tiep theo se bao:
--   ERROR: duplicate key value violates unique constraint "pk_users"
-- RESTART WITH n = gia tri KE TIEP duoc cap phat = max(id) + 1.
-- =====================================================================
ALTER TABLE categories    ALTER COLUMN id RESTART WITH 8;
ALTER TABLE movies        ALTER COLUMN id RESTART WITH 61;
ALTER TABLE users         ALTER COLUMN id RESTART WITH 12;
ALTER TABLE watch_history ALTER COLUMN id RESTART WITH 10;

-- Kiem tra nhanh sau khi migrate:
--   SELECT count(*) FROM users;   -- mong doi 11
--   SELECT max(id)  FROM users;   -- mong doi 11
--   INSERT INTO users (username, email, password, full_name, role)
--       VALUES ('probe', 'probe@local.test', 'x', 'Probe', 'CUSTOMER'); -- id = 12
-- =====================================================================
