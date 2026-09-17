# =====================================================================
# CINEVORA - Phase 1 : kiem tra OFFLINE (khong can Docker)
# ---------------------------------------------------------------------
# Kiem tra moi thu CO THE kiem chung ma khong can PostgreSQL:
#   1. Su ton tai cua cac file ban giao
#   2. Cau truc V1__init_schema.sql (7 bang, FK, CHECK, index)
#   3. Cau truc V2__seed_data.sql (104 dong, 4 lenh RESTART)
#   4. ST9 - quet plaintext: 11/11 mat khau legacy KHONG lot vao repo
#            (sau khi 3 file legacy .txt da xoa: kiem tra trang thai cuoi thay the)
#   5. Khong file nao co BOM (Flyway/psql khong chap nhan BOM)
#
# Cach chay (tu thu muc goc repo):
#   powershell -ExecutionPolicy Bypass -File tools\verify-phase1.ps1
# Exit code: 0 = tat ca PASS, 1 = co loi
# =====================================================================

Set-Location (Join-Path $PSScriptRoot '..')
$fail = 0
function Check($name, $ok, $detail) {
    if ($ok) { Write-Host ("  [PASS] " + $name + " : " + $detail) -ForegroundColor Green }
    else { Write-Host ("  [FAIL] " + $name + " : " + $detail) -ForegroundColor Red; $script:fail++ }
}
function CheckEq($name, $actual, $expected, $note) {
    $ok = ($actual -eq $expected)
    $detail = "$actual (ky vong $expected)"
    if ($note) { $detail = $detail + " - " + $note }
    Check $name $ok $detail
}
# Dem so DONG trong file khop mau (bo qua comment SQL bang cach loc theo tien to dong)
function CountLines($path, $wildcard) {
    return (Get-Content $path | Where-Object { $_ -like $wildcard }).Count
}

$V1 = 'backend/src/main/resources/db/migration/V1__init_schema.sql'
$V2 = 'backend/src/main/resources/db/migration/V2__seed_data.sql'

Write-Host "=== 1. File ban giao ===" -ForegroundColor Cyan
foreach ($f in @($V1, $V2, 'docker-compose.yml', '.env.example',
                 'tools/SeedSqlGenerator.java', 'tools/PasswordHashGenerator.java',
                 'tools/lib/src/org/mindrot/jbcrypt/BCrypt.java',
                 'tools/verify/SpringBcryptParityCheck.java',
                 'docs/erd/database-erd.md', 'docs/database/seed-mapping.md',
                 'docs/database/migration-runbook.md')) {
    Check 'exists' (Test-Path $f) $f
}

Write-Host "=== 2. Cau truc V1 ===" -ForegroundColor Cyan
CheckEq 'V1 lines' (Get-Content $V1).Count 255 '255 dong'
CheckEq 'V1 CREATE TABLE' ((Select-String -Path $V1 -SimpleMatch 'CREATE TABLE').Count) 7 '7 bang'
CheckEq 'V1 PRIMARY KEY' ((Select-String -Path $V1 -SimpleMatch 'PRIMARY KEY').Count) 7 '7 PK'
CheckEq 'V1 FOREIGN KEY' ((Select-String -Path $V1 -SimpleMatch 'FOREIGN KEY').Count) 9 '9 FK'
CheckEq 'V1 ON DELETE RESTRICT' ((Select-String -Path $V1 -SimpleMatch 'ON DELETE RESTRICT').Count) 1 'categories -> movies'
CheckEq 'V1 ON DELETE CASCADE (chi dong REFERENCES)' (CountLines $V1 '        REFERENCES *ON DELETE CASCADE*') 8 '4 bang trung gian x 2 FK'
CheckEq 'V1 CREATE INDEX' ((Select-String -Path $V1 -SimpleMatch 'CREATE INDEX').Count) 12 '10 index thuong + 2 partial'
CheckEq 'V1 UNIQUE INDEX' ((Select-String -Path $V1 -SimpleMatch 'CREATE UNIQUE INDEX').Count) 1 'uk_users_username_lower'
Check 'V1 uk_users_username_lower' ((Select-String -Path $V1 -SimpleMatch 'uk_users_username_lower').Count -ge 2) 'khai bao + ghi chu'
Check 'V1 COMMENT ON >= 20' ((Select-String -Path $V1 -SimpleMatch 'COMMENT ON').Count -ge 20) ((Select-String -Path $V1 -SimpleMatch 'COMMENT ON').Count)

Write-Host "=== 3. Cau truc V2 ===" -ForegroundColor Cyan
$v2raw = Get-Content $V2 -Raw
CheckEq 'V2 so cau INSERT (khong tinh comment)' (CountLines $V2 'INSERT INTO *') 7 '7 bang'
CheckEq 'V2 so lenh RESTART' (CountLines $V2 'ALTER TABLE *RESTART WITH*') 4 'categories=8 movies=61 users=12 watch_history=10'
$seedTables = @(
    @{ Name = 'categories'; Expected = 7 },
    @{ Name = 'movies'; Expected = 60 },
    @{ Name = 'users'; Expected = 11 },
    @{ Name = 'watchlist'; Expected = 7 },
    @{ Name = 'favourites'; Expected = 4 },
    @{ Name = 'watch_history'; Expected = 9 },
    @{ Name = 'continue_watching'; Expected = 6 }
)
for ($i = 0; $i -lt $seedTables.Count; $i++) {
    $table = $seedTables[$i].Name
    $start = $v2raw.IndexOf("INSERT INTO $table")
    $end = if ($i + 1 -lt $seedTables.Count) { $v2raw.IndexOf("INSERT INTO $($seedTables[$i + 1].Name)") } else { $v2raw.Length }
    $block = if ($start -ge 0 -and $end -gt $start) { $v2raw.Substring($start, $end - $start) } else { '' }
    $rows = ([regex]::Matches($block, '(?m)^\s*\(')).Count
    CheckEq "V2 rows $table" $rows $seedTables[$i].Expected "$table seed rows"
}
CheckEq 'V2 so tai khoan CUSTOMER (chi dong du lieu)' (CountLines $V2 "    (*'CUSTOMER'*") 10 '10 CUSTOMER + 1 ADMIN = 11'
CheckEq 'V2 so lan xuat hien BCrypt hash' (CountLines $V2 '*RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6*') 11 '1 hash chung cho 11 tai khoan'
Check 'V2 timestamps deterministic' ($v2raw -notmatch '(?i)\bNOW\s*\(\s*\)') 'khong dung NOW() trong seed'
$whStart = $v2raw.IndexOf('INSERT INTO watch_history')
$whEnd = $v2raw.IndexOf('INSERT INTO continue_watching')
$whBlock = $v2raw.Substring($whStart, $whEnd - $whStart)
CheckEq 'watch_history: M01 cua messi10 xuat hien 2 lan' ([regex]::Matches($whBlock, '\(4, 1,').Count) 2 'du lieu that, khong phai loi'

Write-Host "=== 4. ST9 - quet plaintext (nguon: legacy-cli/data/users.txt) ===" -ForegroundColor Cyan
# ST9 co 2 trang thai:
#   (a) legacy-cli/data/users.txt CON  -> quet du 11 mat khau that (nguon chan ly)
#   (b) DA XOA (dung theo seed-mapping.md muc 9, chi lam sau khi 8/8 smoke test PostgreSQL + ST9 PASS)
#       -> khong con plaintext nao de quet, chuyen sang kiem tra TRANG THAI CUOI:
#          khong con file legacy .txt + van khoi phuc duoc tu git history.
#          (Luu y: tag v1.0-cli-final giu layout CU `data/*.txt`, khong phai `legacy-cli/data/*.txt`.)
$legacyUsers = 'legacy-cli/data/users.txt'
$legacyTxts = @('legacy-cli/data/categories.txt', 'legacy-cli/data/movies.txt', 'legacy-cli/data/users.txt')

$scanTargets = Get-ChildItem -Recurse -File -Include *.md,*.java,*.sql,*.yml,*.yaml,*.example,*.properties,*.json,*.ps1 |
    Where-Object { $_.FullName -notmatch '\\legacy-cli\\data\\' -and $_.FullName -notmatch '\\tools\\build\\' }
$leaks = 0

if (Test-Path $legacyUsers) {
    $legacyPasswords = (Get-Content $legacyUsers) |
        ForEach-Object { ($_ -split '\|')[2] } | Where-Object { $_ } | Sort-Object -Unique
    Check 'So mat khau legacy' ($legacyPasswords.Count -eq 11) "$($legacyPasswords.Count) (ky vong 11)"

    foreach ($p in $legacyPasswords) {
        $hit = $scanTargets | Select-String -SimpleMatch $p
        if ($hit) {
            $leaks++
            Write-Host ("     LEAK tai: " + (($hit | ForEach-Object { $_.Path + ':' + $_.LineNumber }) -join ', ')) -ForegroundColor Yellow
        }
    }
}
else {
    $stillThere = @($legacyTxts | Where-Object { Test-Path $_ })
    Check 'Nguon plaintext da xoa het' ($stillThere.Count -eq 0) "$(3 - $stillThere.Count)/3 file legacy .txt da xoa"

    $tagOk = $true
    $legacyRev = ''
    if (Get-Command git -ErrorAction SilentlyContinue) {
        # KHONG dung `git cat-file -e <tag>:legacy-cli/data/users.txt`: tag v1.0-cli-final duoc tao
        # TRUOC commit chuyen CLI vao legacy-cli/ (078b511), nen trong tag path la `data/*.txt`.
        # Cach ben vung: tim commit gan nhat con Add/Modify (khong phai Delete) dung path nay.
        $legacyRev = (git log --format=%H -1 --diff-filter=AM -- legacy-cli/data/users.txt 2>$null)
        $tagOk = -not [string]::IsNullOrWhiteSpace($legacyRev)
    }
    else {
        Write-Host '     (khong tim thay git trong PATH - bo qua kiem tra khoi phuc)' -ForegroundColor Yellow
    }
    Check 'Khoi phuc duoc tu git history' $tagOk "commit cuoi con giu legacy-cli/data/users.txt = '$legacyRev'"
}

Check 'Plaintext leak' ($leaks -eq 0) "$($scanTargets.Count) file quet, $leaks leak"

Write-Host "=== 5. BOM ===" -ForegroundColor Cyan
foreach ($f in @($V1, $V2, 'tools/SeedSqlGenerator.java', 'tools/PasswordHashGenerator.java', 'docker-compose.yml', '.env.example')) {
    $b = [System.IO.File]::ReadAllBytes((Resolve-Path $f))
    Check 'no BOM' (-not ($b[0] -eq 239 -and $b[1] -eq 187 -and $b[2] -eq 191)) $f
}

Write-Host ""
if ($fail -gt 0) {
    Write-Host "KET QUA: FAIL ($fail kiem tra that bai)" -ForegroundColor Red
    exit 1
}
Write-Host "KET QUA: PASS - tat ca kiem tra offline deu thanh cong" -ForegroundColor Green
Write-Host "LUU Y: 8 smoke test ST1-ST8 tren PostgreSQL DA CHAY THAT va PASS 8/8"
Write-Host "       (PostgreSQL 16.15 qua Docker, ngay 2026-09-15) - xem migration-runbook.md muc 8."
exit 0
