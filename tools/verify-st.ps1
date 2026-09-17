# =====================================================================
# CINEVORA - Smoke test ST1..ST8 tren PostgreSQL
# ---------------------------------------------------------------------
# Chay DUNG 8 kiem tra trong docs/database/migration-runbook.md muc 4.
# Yeu cau: Docker dang chay + da `docker compose up -d postgres` (healthy)
#          + da chay `flyway migrate`.
#
# Su dung:
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\verify-st.ps1
#
# Exit code: 0 = 8/8 PASS, 1 = co it nhat 1 FAIL.
#
# Credentials/database name are read from the running postgres container, so
# the smoke test also works when .env overrides POSTGRES_USER/POSTGRES_DB.
#
# LUU Y ve ST5: test nay chi dung khi DB "fresh" (vua migrate, chua tung
# chay probe). Neu da chay script nay 1 lan, sequence users da bi day len
# 13 -> can `docker compose down -v` roi migrate lai truoc khi chay lai.
#
# ST9 (quet plaintext) khong nam trong script nay vi khong can DB:
#   -> tools\verify-phase1.ps1
# =====================================================================

$ErrorActionPreference = 'Continue'

# Ve goc repo (script nam trong tools\) de `docker compose` tim thay compose file.
Set-Location (Split-Path $PSScriptRoot -Parent)

# Docker Desktop cai theo user -> PATH cua session co the chua co docker.exe.
$dockerBin = Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\resources\bin'
if (Test-Path $dockerBin -ErrorAction SilentlyContinue) { $env:Path = "$dockerBin;$env:Path" }

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host '  [FAIL] Docker CLI khong ton tai trong PATH' -ForegroundColor Red
    exit 1
}

$script:Pass = 0
$script:Fail = 0

function Check {
    param([string]$Id, [bool]$Ok, [string]$Detail)
    if ($Ok) {
        $script:Pass++
        Write-Host ("  [PASS] {0} : {1}" -f $Id, $Detail)
    }
    else {
        $script:Fail++
        Write-Host ("  [FAIL] {0} : {1}" -f $Id, $Detail)
    }
}

# Chay 1 cau SQL bang psql trong container, tra ve exit code + output.
function Invoke-Psql {
    param([string]$Sql)
    $raw = & docker compose exec -T -e PGCLIENTENCODING=UTF8 postgres `
        psql -U $script:DbUser -d $script:DbName -v ON_ERROR_STOP=1 -t -A -c $Sql 2>&1
    $code = $LASTEXITCODE
    $text = (($raw | ForEach-Object { $_.ToString() }) -join "`n").Trim()
    return [pscustomobject]@{ Code = $code; Text = $text }
}

# Dong loi dau tien (ERROR/DETAIL) de bao cao gon.
function Get-ErrLine {
    param([string]$Text)
    $line = ($Text -split "`n" | Where-Object { $_ -match 'ERROR:|DETAIL:' } | Select-Object -First 1)
    if (-not $line) { $line = ($Text -split "`n" | Select-Object -First 1) }
    return $line.Trim()
}

Write-Host ''
Write-Host '=== CINEVORA - Smoke test ST1..ST8 (PostgreSQL) ==='
Write-Host ''

# Resolve the effective values from the container instead of assuming the
# defaults from .env.example. Fall back to defaults only for clearer output
# when postgres is not reachable yet.
$script:DbUser = ((& docker compose exec -T postgres printenv POSTGRES_USER 2>$null) -join '').Trim()
$script:DbName = ((& docker compose exec -T postgres printenv POSTGRES_DB 2>$null) -join '').Trim()
if ([string]::IsNullOrWhiteSpace($script:DbUser)) { $script:DbUser = 'postgres' }
if ([string]::IsNullOrWhiteSpace($script:DbName)) { $script:DbName = 'cinevora_db' }
Write-Host ("Database target: {0}@{1}" -f $script:DbUser, $script:DbName)

# ---------------------------------------------------------------------
Write-Host '--- ST1 - Flyway da ghi nhan du 2 version ---'
$r = Invoke-Psql @'
SELECT string_agg(installed_rank || ':' || version || ':' || CASE WHEN success THEN 't' ELSE 'f' END, ' ' ORDER BY installed_rank)
FROM flyway_schema_history;
'@
Check 'ST1' ($r.Code -eq 0 -and $r.Text -eq '1:1:t 2:2:t') `
    ("flyway_schema_history = '{0}' (ky vong '1:1:t 2:2:t')" -f $r.Text)

# ---------------------------------------------------------------------
Write-Host '--- ST2 - Du 7 bang va dung 104 dong du lieu ---'
$r = Invoke-Psql @'
SELECT (SELECT count(*) FROM categories) || '/' || (SELECT count(*) FROM movies) || '/' || (SELECT count(*) FROM users)
    || '/' || (SELECT count(*) FROM watchlist) || '/' || (SELECT count(*) FROM favourites)
    || '/' || (SELECT count(*) FROM watch_history) || '/' || (SELECT count(*) FROM continue_watching);
'@
$st2Total = 'n/a'
if ($r.Text -match '^[0-9]+(/[0-9]+)*$') {
    $st2Total = (($r.Text -split '/') | ForEach-Object { [int]$_ } | Measure-Object -Sum).Sum
}
Check 'ST2' ($r.Code -eq 0 -and $r.Text -eq '7/60/11/7/4/9/6') `
    ("cat/mov/usr/wl/fav/wh/cw = '{0}' (ky vong '7/60/11/7/4/9/6', tong {1} = 104)" -f $r.Text, $st2Total)

# ---------------------------------------------------------------------
Write-Host '--- ST3 - Khong co ban ghi mo coi (toan ven FK) ---'
$r = Invoke-Psql @'
SELECT (SELECT count(*) FROM movies m LEFT JOIN categories c ON c.id = m.category_id WHERE c.id IS NULL)
    || '/' || (SELECT count(*) FROM watchlist w LEFT JOIN users u ON u.id = w.user_id
               LEFT JOIN movies m ON m.id = w.movie_id WHERE u.id IS NULL OR m.id IS NULL)
    || '/' || (SELECT count(*) FROM favourites f LEFT JOIN users u ON u.id = f.user_id
               LEFT JOIN movies m ON m.id = f.movie_id WHERE u.id IS NULL OR m.id IS NULL)
    || '/' || (SELECT count(*) FROM watch_history h LEFT JOIN users u ON u.id = h.user_id
               LEFT JOIN movies m ON m.id = h.movie_id WHERE u.id IS NULL OR m.id IS NULL)
    || '/' || (SELECT count(*) FROM continue_watching c LEFT JOIN users u ON u.id = c.user_id
               LEFT JOIN movies m ON m.id = c.movie_id WHERE u.id IS NULL OR m.id IS NULL);
'@
Check 'ST3' ($r.Code -eq 0 -and $r.Text -eq '0/0/0/0/0') `
    ("orphans mov/wl/fav/wh/cw = '{0}' (ky vong '0/0/0/0/0')" -f $r.Text)

# ---------------------------------------------------------------------
# THU TU THUC THI: ST5 chay TRUOC ST4 (co chu y).
# ST4 co tinh vi pham constraint, nhung PostgreSQL da goi nextval() cho cot
# IDENTITY TRUOC khi kiem tra rang buoc -> sequence bi tieu ton 1 gia tri/bang.
# Neu ST4 chay truoc, ST5 se thay id ke tiep = 13/62 thay vi 12/61 => FAIL du sai.
# ---------------------------------------------------------------------
Write-Host '--- ST5 - Identity sequence da RESTART (chong duplicate key o Phase 2) ---'

# Don dep phong khi lan chay truoc bi ngat giua chung.
Invoke-Psql "DELETE FROM users WHERE username = 'probe_seed_check';" | Out-Null
$r = Invoke-Psql @'
WITH ins AS (
    INSERT INTO users (username, email, password, full_name, role)
    VALUES ('probe_seed_check', 'probe@local.test', 'x', 'Probe', 'CUSTOMER')
    RETURNING id
) SELECT id FROM ins;
'@
$st5UserId = $r.Text
Invoke-Psql "DELETE FROM users WHERE username = 'probe_seed_check';" | Out-Null
Check 'ST5a' ($r.Code -eq 0 -and $st5UserId -eq '12') `
    ("users.id ke tiep = '{0}' (ky vong '12'; neu la '13' -> DB khong con 'fresh' hoac ST4 da chay truoc, chay 'docker compose down -v' + migrate lai)" -f $st5UserId)

Invoke-Psql "DELETE FROM movies WHERE title = 'Probe Movie';" | Out-Null
$r = Invoke-Psql @'
WITH ins AS (
    INSERT INTO movies (category_id, title, director, actors, release_year, rating)
    VALUES (1, 'Probe Movie', 'X', 'Y', 2020, 1.0)
    RETURNING id
) SELECT id FROM ins;
'@
$st5MovieId = $r.Text
Invoke-Psql "DELETE FROM movies WHERE title = 'Probe Movie';" | Out-Null
Check 'ST5b' ($r.Code -eq 0 -and $st5MovieId -eq '61') `
    ("movies.id ke tiep = '{0}' (ky vong '61')" -f $st5MovieId)

# ---------------------------------------------------------------------
# ST4 chay SAU ST5 vi ly do o tren (ST4 tieu ton gia tri sequence).
Write-Host '--- ST4 - Rang buoc thuc su hoat dong (3 cau PHAI LOI) ---'

# 4a. UNIQUE username khong phan biet hoa/thuong -> duplicate key
$r = Invoke-Psql @'
INSERT INTO users (username, email, password, full_name, role)
VALUES ('ADMIN', 'x1@local.test', 'x', 'X', 'CUSTOMER');
'@
Check 'ST4a' ($r.Code -ne 0 -and $r.Text -match 'duplicate key') `
    ("UNIQUE(LOWER(username)) phai loi duplicate key -> exit={0} | {1}" -f $r.Code, (Get-ErrLine $r.Text))

# 4b. FK -> violates foreign key constraint
$r = Invoke-Psql @'
INSERT INTO movies (category_id, title, director, actors, release_year, rating)
VALUES (999, 'Phim Sai The Loai', 'X', 'Y', 2020, 5.0);
'@
Check 'ST4b' ($r.Code -ne 0 -and $r.Text -match 'foreign key constraint') `
    ("FK category_id=999 phai loi foreign key -> exit={0} | {1}" -f $r.Code, (Get-ErrLine $r.Text))

# 4c. CHECK percent 0..100 -> violates check constraint
$r = Invoke-Psql @'
INSERT INTO continue_watching (user_id, movie_id, percent) VALUES (2, 3, 150);
'@
Check 'ST4c' ($r.Code -ne 0 -and $r.Text -match 'check constraint') `
    ("CHECK percent=150 phai loi check constraint -> exit={0} | {1}" -f $r.Code, (Get-ErrLine $r.Text))

# ---------------------------------------------------------------------
Write-Host '--- ST6 - Index da duoc tao day du ---'
$r = Invoke-Psql @'
SELECT count(*) FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN ('uk_users_username_lower', 'idx_movies_category_id', 'idx_movies_views',
                    'idx_watchlist_movie_id', 'idx_favourites_movie_id', 'idx_watch_history_movie_id',
                    'idx_continue_watching_movie_id', 'idx_watch_history_user_id');
'@
Check 'ST6' ($r.Code -eq 0 -and $r.Text -eq '8') `
    ("8 index bat buoc (FK + query pattern) = {0} (ky vong 8)" -f $r.Text)

# ---------------------------------------------------------------------
Write-Host '--- ST7 - Mat khau da duoc bam dung chuan BCrypt ---'
$r = Invoke-Psql @'
SELECT count(*) || '/' || count(*) FILTER (WHERE password LIKE '$2a$10$%')
    || '/' || count(*) FILTER (WHERE length(password) = 60) || '/' || count(DISTINCT password)
FROM users;
'@
Check 'ST7' ($r.Code -eq 0 -and $r.Text -eq '11/11/11/1') `
    ("tong/dung-tien-to-2a-10/dung-do-dai-60/so-hash-khac-nhau = '{0}' (ky vong '11/11/11/1')" -f $r.Text)

# ---------------------------------------------------------------------
Write-Host '--- ST8 - Tieng Viet & index LOWER(username) hoat dong ---'
# Dung U&'...' (unicode escape) de tranh phu thuoc encoding cua console.
$r = Invoke-Psql @'
SELECT (SELECT full_name = U&'\0110\1ed7 Thi\1ebft Th\1ea1ch' FROM users WHERE id = 2)
    || '/' || (SELECT username FROM users WHERE LOWER(username) = LOWER('ADMIN'))
    || '/' || (SELECT name = U&'H\00e0nh \0110\1ed9ng' FROM categories ORDER BY id LIMIT 1);
'@
Check 'ST8' ($r.Code -eq 0 -and $r.Text -eq 'true/admin/true') `
    ("users.id=2 = 'Do Thiet Thach' (du dau) / LOWER(username)='ADMIN' -> admin / categories.id=1 = 'Hanh Dong' (du dau) : '{0}' (ky vong 'true/admin/true')" -f $r.Text)

# ---------------------------------------------------------------------
Write-Host ''
Write-Host ("KET QUA: {0} PASS, {1} FAIL" -f $script:Pass, $script:Fail)
if ($script:Fail -eq 0) {
    Write-Host 'KET QUA: 8/8 SMOKE TEST PASS (ST1..ST8)'
    exit 0
}
else {
    Write-Host 'KET QUA: FAIL - xem chi tiet o tren. KHONG ket luan Phase 1 hoan tat.'
    exit 1
}


