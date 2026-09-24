# Migration Runbook — Phase 1

> Cách chạy, kiểm tra và khôi phục migration database của Cinevora.
> Liên quan: [`V1__init_schema.sql`](../../backend/src/main/resources/db/migration/V1__init_schema.sql) ·
> [`V2__seed_data.sql`](../../backend/src/main/resources/db/migration/V2__seed_data.sql) ·
> [`seed-mapping.md`](seed-mapping.md) · [`database-erd.md`](../erd/database-erd.md)

---

## 1. Điều Kiện Tiên Quyết

| Thành phần | Bắt buộc? | Dùng để |
|---|---|---|
| **Docker Desktop** (hoặc Docker Engine + Compose v2) | ✅ Cho migration | Chạy PostgreSQL 16 + Flyway CLI trong container |
| **JDK 21** | ✅ Cho tools | Biên dịch/dùng `tools/*.java` (sinh hash, sinh seed) |
| PowerShell 5.1+ / bash | ✅ | Chạy lệnh |
| `psql` trên máy host | ❌ Không cần | Dùng `docker compose exec postgres psql` |
| Maven / Node | ❌ Không cần | Phase 1 không dùng |

> [!NOTE]
> Trên Windows, `java` trong PATH có thể trỏ tới **JRE 1.8** (cũ). Luôn gọi `java`/`javac` bằng
> **đường dẫn đầy đủ trong thư mục JDK 21**, nếu không sẽ gặp
> `UnsupportedClassVersionError: class file version 65.0`.

> [!NOTE]
> **Docker Desktop thường cài theo user (per-user)** → `docker.exe` nằm ở
> `%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin`, **không** phải `C:\Program Files\Docker\Docker\...`.
> Nếu terminal/IDE được mở **trước khi** cài Docker Desktop, tiến trình đó giữ PATH cũ và bạn sẽ gặp
> `docker : The term 'docker' is not recognized` **dù `docker run hello-world` vẫn chạy ở cửa sổ khác**.
> Khắc phục: mở terminal mới, hoặc nạp lại PATH ngay trong session hiện tại:
>
> ```powershell
> $env:Path = "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin;$env:Path"
> ```
>
> `tools/verify-st.ps1` tự nạp đường dẫn này nên không cần làm thủ công.

---

## 2. Cấu Trúc File

```
docker-compose.yml                                   # postgres:16-alpine + flyway (profile tools)
.env.example                                         # mẫu biến môi trường (copy thành .env)
backend/src/main/resources/db/migration/
├── V1__init_schema.sql                              # 7 bảng + constraint + index + comment
└── V2__seed_data.sql                                # 104 dòng seed + RESTART identity
tools/
├── lib/src/org/mindrot/jbcrypt/BCrypt.java          # jBCrypt 0.4 vendor (0 dependency)
├── PasswordHashGenerator.java                       # sinh/kiểm tra hash BCrypt
├── SeedSqlGenerator.java                            # sinh V2 từ legacy-cli/data/*.txt
├── verify-phase1.ps1                                # kiểm tra OFFLINE toàn bộ (30 check, không cần Docker)
├── verify-st.ps1                                    # SMOKE TEST ST1..ST8 trên PostgreSQL (cần Docker)
└── verify/SpringBcryptParityCheck.java              # kiểm chứng độc lập bằng Spring Security
```

---

## 3. Chạy Migration

### 3.1. Khởi động PostgreSQL

```powershell
Copy-Item .env.example .env      # lần đầu
docker compose up -d postgres
docker compose ps                # chờ STATUS = healthy
```

### 3.2. Chạy migration bằng Flyway CLI trong Docker

Không cần cài Java/Maven trên máy host — Flyway chạy trong container và mount thẳng thư mục migration:

```powershell
docker compose --profile tools run --rm flyway `
    migrate
```

Kết quả mong đợi:

```
Successfully applied 2 migrations to schema "public", now at version v2
```

### 3.3. Đối chiếu trạng thái (idempotency)

```powershell
docker compose --profile tools run --rm flyway `
    info
```

Chạy `migrate` lần thứ hai **không được** áp dụng lại gì
(`Schema "public" is up to date. No migration necessary.`) và `info` phải hiển thị cả `1` và `2`
ở trạng thái `Success`.

---

## 4. 8 PostgreSQL Smoke Test + ST9 Offline (BẮT BUỘC PASS)

### Cách nhanh — chạy script tự động

```powershell
& "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File tools\verify-st.ps1
# Exit code 0 = 8/8 PASS (ST1..ST8) · 1 = có FAIL
```

`tools/verify-st.ps1` chạy **đúng 8 kiểm tra PostgreSQL ST1–ST8 mô tả dưới đây**, in `[PASS]/[FAIL]`
cho từng test và tự dọn dẹp dữ liệu probe. ST9 là kiểm tra plaintext offline trong
`tools/verify-phase1.ps1`, không chạy trên PostgreSQL.

> [!WARNING]
> **ST5 chỉ đúng khi DB còn "fresh"** (vừa migrate, chưa từng chạy probe) **và phải chạy TRƯỚC ST4**.
> Lý do: PostgreSQL gọi `nextval()` cho cột `GENERATED … AS IDENTITY` **trước khi** kiểm tra
> `UNIQUE`/`FK`/`CHECK`, nên mỗi câu INSERT cố tình sai của ST4 **vẫn tiêu tốn 1 giá trị sequence**.
> Nếu ST4 chạy trước, ST5 sẽ thấy `id` kế tiếp là `13`/`62` thay vì `12`/`61` → cần
> `docker compose down -v` rồi migrate lại trước khi chạy lại.

### Đặt shortcut cho `psql` trong container

```powershell
$DbUser = ((docker compose exec -T postgres printenv POSTGRES_USER) -join '').Trim()
$DbName = ((docker compose exec -T postgres printenv POSTGRES_DB) -join '').Trim()
function pg { docker compose exec -T postgres psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 @args }
```

### ST1 — Flyway đã ghi nhận đủ 2 version

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;
-- Kỳ vọng: 2 dòng, version 1 & 2, success = t
```

### ST2 — Đủ 7 bảng và đúng 104 dòng dữ liệu

```sql
SELECT 'categories' AS t, count(*) AS n FROM categories
UNION ALL SELECT 'movies',             count(*) FROM movies
UNION ALL SELECT 'users',              count(*) FROM users
UNION ALL SELECT 'watchlist',          count(*) FROM watchlist
UNION ALL SELECT 'favourites',         count(*) FROM favourites
UNION ALL SELECT 'watch_history',      count(*) FROM watch_history
UNION ALL SELECT 'continue_watching',  count(*) FROM continue_watching
ORDER BY t;
-- Kỳ vọng: 7 / 60 / 11 / 7 / 4 / 9 / 6  → tổng 104
```

### ST3 — Không có bản ghi mồ côi (toàn vẹn FK)

```sql
SELECT
 (SELECT count(*) FROM movies m LEFT JOIN categories c ON c.id = m.category_id
   WHERE c.id IS NULL)                                                            AS orphan_movies,
 (SELECT count(*) FROM watchlist w LEFT JOIN users u ON u.id = w.user_id
   LEFT JOIN movies m ON m.id = w.movie_id WHERE u.id IS NULL OR m.id IS NULL)    AS orphan_watchlist,
 (SELECT count(*) FROM favourites f LEFT JOIN users u ON u.id = f.user_id
   LEFT JOIN movies m ON m.id = f.movie_id WHERE u.id IS NULL OR m.id IS NULL)    AS orphan_favourites,
 (SELECT count(*) FROM watch_history h LEFT JOIN users u ON u.id = h.user_id
   LEFT JOIN movies m ON m.id = h.movie_id WHERE u.id IS NULL OR m.id IS NULL)    AS orphan_history,
 (SELECT count(*) FROM continue_watching c LEFT JOIN users u ON u.id = c.user_id
   LEFT JOIN movies m ON m.id = c.movie_id WHERE u.id IS NULL OR m.id IS NULL)    AS orphan_progress;
-- Kỳ vọng: tất cả = 0
```

### ST4 — Ràng buộc thực sự hoạt động (3 phép thử PHẢI LỖI)

> [!CAUTION]
> **Thứ tự bắt buộc: ST5 → ST4.** 3 câu dưới đây cố tình vi phạm ràng buộc, nhưng PostgreSQL đã
> cấp `nextval()` cho cột `id` **trước khi** phát hiện lỗi → mỗi câu **vẫn tiêu tốn 1 giá trị
> sequence** (`users_id_seq` 12→13, `movies_id_seq` 61→62). Chạy ST4 trước ST5 sẽ làm ST5 báo sai.
> Nếu lỡ chạy sai thứ tự: `docker compose down -v` rồi migrate lại.

```sql
-- 4a. UNIQUE username không phân biệt hoa/thường -> phải lỗi duplicate key
INSERT INTO users (username, email, password, full_name, role)
VALUES ('ADMIN', 'x1@local.test', 'x', 'X', 'CUSTOMER');

-- 4b. FK -> phải lỗi "violates foreign key constraint"
INSERT INTO movies (category_id, title, director, actors, release_year, rating)
VALUES (999, 'Phim Sai The Loai', 'X', 'Y', 2020, 5.0);

-- 4c. CHECK percent 0..100 -> phải lỗi "violates check constraint"
INSERT INTO continue_watching (user_id, movie_id, percent) VALUES (2, 3, 150);
```

> Cả 3 câu **phải thất bại**. Nếu câu nào chạy trôi chảy nghĩa là constraint/index chưa đúng.
> Vì `ON_ERROR_STOP=1` dừng ở lỗi đầu tiên, hãy chạy **từng câu riêng** để kiểm tra đủ 3.

Kết quả thật trên PostgreSQL 16.15 (`exit=1` cho cả 3):

```
ERROR:  duplicate key value violates unique constraint "uk_users_username_lower"
ERROR:  insert or update on table "movies" violates foreign key constraint "fk_movies_categories"
ERROR:  new row for relation "continue_watching" violates check constraint "chk_continue_watching_percent"
```

### ST5 — Identity sequence đã RESTART (chống lỗi duplicate key ở Phase 2)

> [!IMPORTANT]
> **Chạy ST5 TRƯỚC ST4** và chỉ đúng trên DB **vừa migrate** (chưa từng chạy probe).
> Dùng CTE để `RETURNING` không in kèm dòng `INSERT 0 1` (dễ làm sai kết quả so khớp).

```sql
-- 5a. users: kỳ vọng id = 12
WITH ins AS (
    INSERT INTO users (username, email, password, full_name, role)
    VALUES ('probe_seed_check', 'probe@local.test', 'x', 'Probe', 'CUSTOMER')
    RETURNING id
) SELECT id FROM ins;
-- Kỳ vọng: 12. Nếu ra 1 hoặc báo "duplicate key" -> V2 thiếu ALTER ... RESTART WITH
DELETE FROM users WHERE username = 'probe_seed_check';

-- 5b. movies: kỳ vọng id = 61
WITH ins AS (
    INSERT INTO movies (category_id, title, director, actors, release_year, rating)
    VALUES (1, 'Probe Movie', 'X', 'Y', 2020, 1.0)
    RETURNING id
) SELECT id FROM ins;
-- Kỳ vọng: 61
DELETE FROM movies WHERE title = 'Probe Movie';
```

Kết quả thật trên PostgreSQL 16.15: `12` và `61` (khớp `RESTART WITH 12` / `RESTART WITH 61` của V2).

### ST6 — Index đã được tạo đầy đủ

```sql
SELECT indexname FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN ('uk_users_username_lower','idx_movies_category_id','idx_movies_views',
                    'idx_watchlist_movie_id','idx_favourites_movie_id','idx_watch_history_movie_id',
                    'idx_continue_watching_movie_id','idx_watch_history_user_id')
ORDER BY indexname;
-- Kỳ vọng: đủ 8 dòng
```

### ST7 — Mật khẩu đã được băm đúng chuẩn BCrypt

```sql
SELECT count(*)                                          AS tong_tai_khoan,
       count(*) FILTER (WHERE password LIKE '$2a$10$%')  AS dung_tien_to,
       count(*) FILTER (WHERE length(password) = 60)     AS dung_do_dai,
       count(DISTINCT password)                          AS so_hash_khac_nhau
FROM users;
-- Kỳ vọng: 11 / 11 / 11 / 1   (1 hash chung cho toàn bộ tài khoản demo)
```

### ST8 — Tiếng Việt & index `LOWER(username)` hoạt động

```sql
SELECT full_name FROM users WHERE id = 2;                          -- Đỗ Thiết Thạch (đủ dấu)
SELECT username FROM users WHERE LOWER(username) = LOWER('ADMIN'); -- admin (đăng nhập không phân biệt hoa/thường)
SELECT name FROM categories ORDER BY id LIMIT 1;                   -- Hành Động
```

> [!TIP]
> Khi kiểm tra bằng script/`-t -A`, lưu ý `boolean || text` trong PostgreSQL cho ra `true`/`false`
> (KHÔNG phải `t`/`f` như khi `psql` hiển thị một cột boolean). `tools/verify-st.ps1` so khớp
> chuỗi `true/admin/true` và dùng `U&'\0110\1ed7 …'` (unicode escape, thuần ASCII) để không phụ
> thuộc code page của console Windows.
> Giá trị thật đã xác nhận: `true/admin/true` → dấu tiếng Việt nguyên vẹn + `LOWER(username)` đúng.

### ST9 — Không còn mật khẩu plaintext ở bất kỳ đâu

Phải quét **đủ cả 11** giá trị (lấy trực tiếp từ `legacy-cli/data/users.txt` cột 3),
**không** chỉ 3 giá trị như bản kế hoạch ban đầu:

```powershell
Set-Location <thu-muc-goc-repo>

# Danh sách "đen" - đọc TRỰC TIẾP từ users.txt, KHÔNG chép tay từ trí nhớ
$legacyPasswords = (Get-Content legacy-cli\data\users.txt) |
    ForEach-Object { ($_ -split '\|')[2] } | Where-Object { $_ } | Sort-Object -Unique
"Tong so mat khau legacy doc duoc: $($legacyPasswords.Count)  (ky vong 11)"
if ($legacyPasswords.Count -ne 11) { throw "FAIL: khong doc du 11 mat khau" }

# Quet TOAN BO repo (tru legacy-cli/data - noi duoc phep chua, se xoa sau khi PASS)
$scanTargets = Get-ChildItem -Recurse -File -Include *.md,*.java,*.sql,*.yml,*.yaml,*.example,*.properties,*.json,*.ps1 |
    Where-Object { $_.FullName -notmatch '\\legacy-cli\\data\\' -and $_.FullName -notmatch '\\tools\\build\\' }

$leaks = 0
foreach ($p in $legacyPasswords) {
    $hit = $scanTargets | Select-String -SimpleMatch $p
    # KHONG in gia tri mat khau ra log - chi in so dong khop va vi tri
    if ($hit) {
        $leaks++
        "LEAK: $($hit.Count) dong"
        $hit | ForEach-Object { "   -> $($_.Path):$($_.LineNumber)" }
    }
}
if ($leaks -gt 0) { "KET QUA: FAIL ($leaks mat khau bi lo)" } else { "KET QUA: PASS (11/11 sach)" }
```

Kết quả đã xác nhận trên repo hiện tại: **PASS — 47 file được quét, 0 leak**.
Mọi file mã nguồn/SQL/cấu hình đều sạch. Trước ngày 2026-09-15, nguồn plaintext **duy nhất** còn lại
là `legacy-cli/data/users.txt`; sau khi 8/8 PostgreSQL smoke test + ST9 PASS, file này (cùng 2 file legacy khác) **đã
bị xoá**, nên từ nay ST9 chạy theo nhánh "trạng thái cuối": không còn file legacy `.txt` và vẫn khôi
phục được từ `v1.0-cli-final`.

> [!NOTE]
> **Trạng thái 2026-09-15:** điều kiện "8/8 PostgreSQL smoke test + ST9 PASS" đã thoả (ST1–ST8 trên PostgreSQL 16.15
> qua Docker + ST9 quét plaintext) nên `legacy-cli/data/{categories,movies,users}.txt` **đã bị xoá**
> bằng `git rm`. `tools/verify-phase1.ps1` tự nhận biết 2 trạng thái: khi file còn → quét đủ 11 mật
> khẩu; khi file đã xoá → kiểm tra trạng thái cuối (không còn file legacy + vẫn khôi phục được từ
> `git tag v1.0-cli-final`). Xem `seed-mapping.md` §9 để khôi phục nguồn dữ liệu khi cần.

---

## 5. Reset & Khôi Phục

```powershell
# Dừng container nhưng GIỮ dữ liệu
docker compose down

# Xoá sạch database + volume, rồi migrate lại từ đầu
docker compose down -v
docker compose up -d postgres
docker compose --profile tools run --rm flyway `
    migrate
```

> **Không có `down` migration.** Flyway community không hỗ trợ undo; cách "rollback" chuẩn là
> `down -v` rồi migrate lại, hoặc tạo migration `V3__*.sql` để sửa tiến (forward-only).
> **Tuyệt đối không sửa `V1`/`V2` đã chạy** — Flyway sẽ báo checksum mismatch và chặn mọi migration sau.

---

## 6. Kiểm Chứng BCrypt Độc Lập (khuyến nghị chạy 1 lần)

V2 sinh hash bằng **jBCrypt 0.4**, nhưng Phase 2 xác thực bằng **`BCryptPasswordEncoder`** của
Spring Security. Hai thư viện khác nhau → phải chứng minh chúng tương thích, nếu không **toàn bộ
tài khoản seed sẽ không đăng nhập được**.

```powershell
# 1. Tải 2 jar (chỉ cần làm 1 lần, có mạng)
New-Item -ItemType Directory -Force -Path tools\build\verify | Out-Null
Invoke-WebRequest 'https://repo1.maven.org/maven2/org/springframework/security/spring-security-crypto/6.3.3/spring-security-crypto-6.3.3.jar' -OutFile tools\build\verify\spring-security-crypto-6.3.3.jar
Invoke-WebRequest 'https://repo1.maven.org/maven2/org/springframework/spring-jcl/6.1.12/spring-jcl-6.1.12.jar' -OutFile tools\build\verify\spring-jcl-6.1.12.jar

# 2. Biên dịch + chạy (đọc trực tiếp hash từ V2__seed_data.sql)
$JDK = "C:\duong\dan\jdk-21\bin"
& "$JDK\javac.exe" -encoding UTF-8 -cp "tools\build\verify\*" -d tools\build\verify-classes tools\verify\SpringBcryptParityCheck.java
& "$JDK\java.exe" -cp "tools\build\verify-classes;tools\build\verify\*" SpringBcryptParityCheck
```

Kết quả đã xác nhận trên repo hiện tại:

```
  [PASS] $2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6  matches=true wrongRejected=true
  [PASS] Spring tu sinh + tu kiem tra: $2a$10$gp0LGdJewOtN3g/Bh7i.Q.34z9S1P4QEnDxFoDB4Ts9gVK40Xy8xu
KET QUA: PASS - hash trong V2 xac thuc duoc bang BCryptPasswordEncoder
```

```powershell
# 3. Chiều ngược lại: jBCrypt đọc được hash do Spring sinh ra
& "$JDK\java.exe" -cp tools\build\classes PasswordHashGenerator --verify "$env:DEMO_PASSWORD" "<hash-do-spring-sinh>"
# Kỳ vọng: ket qua : MATCH
```

---

## 7. Xử Lý Sự Cố

| Triệu chứng | Nguyên nhân | Cách khắc phục |
|---|---|---|
| `duplicate key value violates unique constraint "pk_users"` khi Phase 2 insert | V2 thiếu `ALTER TABLE … RESTART WITH` hoặc chạy sai thứ tự | Kiểm tra cuối V2 có 4 câu `RESTART WITH 8/61/12/10` |
| `FlywayValidateException: Migration checksum mismatch` | Đã sửa `V1`/`V2` sau khi migrate | `docker compose down -v` rồi migrate lại; **không** sửa migration đã chạy |
| Ký tự tiếng Việt thành `??`/`Ã©` | Kết nối không dùng UTF-8 | `POSTGRES_INITDB_ARGS` đã ép `--encoding=UTF8`; kiểm tra client encoding: `SHOW client_encoding;` → `UTF8` |
| `ERROR: syntax error at or near "\ufeff"` | File SQL có BOM | Cả V1/V2 phải là **UTF-8 không BOM**; `SeedSqlGenerator.writeUtf8NoBom()` đảm bảo điều này |
| `Search path "C:\..." could not be found` / tool không chạy | Chạy `java` từ thư mục khác repo root | Luôn chạy `SeedSqlGenerator` từ **thư mục gốc repo** (đường dẫn mặc định là tương đối) |
| `UnsupportedClassVersionError … 65.0` | `java` đang là JRE 1.8 | Gọi đúng `"...\jdk-21\bin\java.exe"` |
| Port 5432 đã bị chiếm | Có PostgreSQL khác đang chạy | Đổi `POSTGRES_PORT=5433` trong `.env` rồi `docker compose up -d postgres` |
| `docker : The term 'docker' is not recognized` dù `docker run hello-world` chạy được | Docker Desktop cài **per-user**; terminal/IDE đang giữ PATH cũ (mở trước khi cài) | `$env:Path = "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin;$env:Path"` rồi chạy lại |
| `no configuration file provided: not found` | Chạy `docker compose` ngoài thư mục gốc repo | `Set-Location` về thư mục gốc repo (nơi có `docker-compose.yml`) |
| `docker compose run flyway` treo ở `Container cinevora-postgres Running` | Đang **pull image** `flyway/flyway:10-alpine` lần đầu (~80 MB, không phải lỗi) | Chờ lần đầu; các lần sau chạy vài giây |

---

## 8. Trạng Thái Thực Thi Trên Máy Phát Triển Hiện Tại

Để minh bạch tiến độ, đây là những gì **đã chạy thật** và những gì **chưa**:

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| Biên dịch `tools/*.java` (JDK 21) | ✅ Đã chạy | `javac exit=0` |
| `PasswordHashGenerator --self-test` | ✅ PASS 37/37 | Round-trip cost 4..12 + kiểm tra âm |
| Sinh `V2__seed_data.sql` | ✅ Đã chạy | 18207 bytes · 172 dòng · SHA-256 `EC697ABF…E728` |
| Tính xác định của generator | ✅ Đã chứng minh | Chạy 2 lần → SHA-256 giống hệt |
| Quét plaintext trong SQL | ✅ 11/11 sạch | `assertNoPlaintext()` của generator |
| Kiểm chứng BCrypt bằng Spring Security | ✅ PASS | `tools/verify/SpringBcryptParityCheck` |
| **Chạy `tools/verify-phase1.ps1`** | ✅ **PASS** | Kiểm tra offline: cấu trúc V1/V2, ST9, BOM, sự tồn tại của file bàn giao |
| **`docker compose up -d postgres`** | ✅ **Đã chạy** | `postgres:16-alpine` → **PostgreSQL 16.15** · healthcheck `healthy` · volume `cinevora_pgdata` |
| **`flyway migrate` trên volume mới** | ✅ **PASS** | `info` = `<< Empty Schema >>` → `migrate` = `Successfully applied 2 migrations … now at version v2` (exit 0) |
| **Idempotency (`migrate` lần 2)** | ✅ **PASS** | `Schema "public" is up to date. No migration necessary.` (exit 0) |
| **8 smoke test ST1–ST8 trên PostgreSQL** | ✅ **PASS 11/11 check** | `tools/verify-st.ps1` → `KET QUA: 8/8 SMOKE TEST PASS` · `ST_EXIT=0` |
| **ST9 — quét plaintext toàn repo** | ✅ **PASS** | Trước khi xoá: 47 file · 11/11 mật khẩu · 0 leak. Sau khi xoá (nhánh "trạng thái cuối"): 49 file quét · 0 leak · nguồn khôi phục còn nguyên (`078b511…`) |
| **Xoá `legacy-cli/data/*.txt`** | ✅ **ĐÃ XOÁ** | `git rm` 3 file (2026-09-15) sau khi 8/8 PostgreSQL smoke test + ST9 PASS — khôi phục: `git checkout $(git log --format=%H -1 --diff-filter=AM -- legacy-cli/data/users.txt) -- legacy-cli/data` |

> [!NOTE]
> **Phase 1 đã hoàn tất 100% trên máy này (2026-09-15).** Trình tự bằng chứng thô:
> `docker compose down -v` → `up -d postgres` → healthcheck `healthy` →
> `flyway info` = `<< Empty Schema >>` → `flyway migrate` = `Successfully applied 2 migrations`,
> `now at version v2` → `migrate` lần 2 = `No migration necessary` →
> `tools/verify-st.ps1` = **11 PASS / 0 FAIL** (`8/8 SMOKE TEST PASS`, exit 0), cộng với ST9
> offline = PASS → `git rm` 3 file legacy theo `seed-mapping.md` §9. Flyway chạy không còn cảnh báo
> `Storing migrations in 'sql'`
> (đã khai báo `FLYWAY_LOCATIONS=filesystem:/flyway/sql` trong `docker-compose.yml`).

### Cập nhật master check — 2026-09-19

Docker Desktop đã được xác nhận khả dụng qua binary per-user trên Windows:

```text
Docker version 29.8.0, build 88096ef
Docker Compose version v5.5.1
Docker Server Version: 29.8.0 · Context: desktop-linux
```

`tools/verify-st.ps1` đã được chạy lại trên database hiện tại và cho kết quả **8 PASS / 3 FAIL**. ST1 (Flyway), ST3 (orphan), ST4a–c (unique/FK/CHECK), ST6 (index), ST7 (BCrypt) và ST8 (Unicode/lowercase) đều PASS. ST2 và ST5a–b FAIL vì database không còn ở trạng thái fresh: số liệu hiện tại là `9/63/11/7/5/11/6` thay vì `7/60/11/7/4/9/6`, sequence kế tiếp là users `45` và movies `97` thay vì `12` và `61`.

Các chênh lệch này đến từ record đã archive và các smoke test thủ công trước đó; không có lỗi migration, foreign key, CHECK, index hoặc BCrypt. Không reset volume để bảo toàn trạng thái phát triển hiện tại. Bằng chứng fresh database **8/8 smoke test PASS** của Phase 1 vẫn được giữ nguyên ở phần trên.

Chi tiết log của lần master check mới nhất xem tại [`docs/verification/MASTER_CHECK_PHASE_1_3.md`](../verification/MASTER_CHECK_PHASE_1_3.md).

---

## 9. Bàn Giao Sang Phase 2

Xem [`database-erd.md`](../erd/database-erd.md) §6 để biết danh sách đầy đủ các quyết định mà
Phase 2 (Spring Boot) **bắt buộc** phải tuân theo — đặc biệt:

* `rating` là `NUMERIC(3,1)` → entity phải map **`BigDecimal`**, không dùng `Double`.
* `spring.jpa.hibernate.ddl-auto=validate` + dependency `flyway-database-postgresql`.
* `role` phải map bằng `@Enumerated(EnumType.STRING)` (hoặc `@DiscriminatorColumn` nếu dùng inheritance), khớp `VARCHAR(20)` và giá trị `ADMIN`/`CUSTOMER`.
* Kiểm thử parity BCrypt trong test suite (dùng chính hash trong V2).



