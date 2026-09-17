import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.mindrot.jbcrypt.BCrypt;

/**
 * CINEVORA - Phase 1 tool : sinh file migration V2__seed_data.sql
 *                          tu du lieu cua ban CLI cu (legacy-cli/data/*.txt).
 *
 * NGUYEN TAC
 *   1. Xac dinh (deterministic): cung du lieu dau vao -> cung file SQL dau ra.
 *      Moi thu tu duoc suy ra tu thu tu dong trong file .txt, khong dung
 *      Random, khong dung System.currentTimeMillis().
 *   2. Bang anh xa ID (xem docs/database/seed-mapping.md):
 *        CATnn (categories.txt) -> n
 *        Mnn   (movies.txt)     -> n
 *        users.txt              -> so thu tu dong (1-based)
 *   3. BAO MAT: mat khau plaintext trong users.txt (cot 3) bi LOAI BO hoan
 *      toan. Moi tai khoan dung chung 1 BCrypt hash cua mat khau demo.
 *      Tool tu kiem tra lai file SQL vua sinh de dam bao khong con plaintext.
 *   4. Sau khi seed xong, phai RESTART identity sequence cua users/categories/
 *      movies/watch_history, neu khong lan INSERT tiep theo se trung khoa
 *      chinh (id bat dau lai tu 1).
 *
 * CACH CHAY (tu thu muc goc cua repo)
 *   & "$JDK\javac.exe" -encoding UTF-8 -d tools\build\classes tools\lib\src\org\mindrot\jbcrypt\BCrypt.java tools\PasswordHashGenerator.java tools\SeedSqlGenerator.java
 *   & "$JDK\java.exe" -cp tools\build\classes SeedSqlGenerator
 *       (tuy chon) SeedSqlGenerator <dataDir> <outSql>
 */
public final class SeedSqlGenerator {

    // ------------------------------------------------------------------
    // Cau hinh
    // ------------------------------------------------------------------
    /** Mat khau demo. CHI dung de verify hash - khong bao gio ghi ra SQL. */
    private static final String DEMO_PASSWORD = "Cinevora@2026";

    /** Hash BCrypt sinh boi PasswordHashGenerator cho DEMO_PASSWORD. */
    private static final String DEMO_PASSWORD_HASH = "$2a$10$RSRK4WMinzwDDi6hcY19lOHClSIggDv1DUOUB.3VEKnFQss0L4CV6";

    /** Moc thoi gian goc - CO DINH de file SQL sinh ra xac dinh (deterministic). */
    private static final String SEED_EPOCH = "2026-01-01T00:00:00+00:00";

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxxx", Locale.ROOT);

    /** Moc goc da parse - co dinh, khong phu thuoc dong ho he thong. */
    private static final OffsetDateTime EPOCH = OffsetDateTime.parse(SEED_EPOCH);

    // So dong ky vong - neu du lieu legacy doi thi tool phai dung lai ngay
    // thay vi am tham sinh ra file seed sai.
    private static final int EXPECTED_CATEGORIES = 7;
    private static final int EXPECTED_MOVIES = 60;
    private static final int EXPECTED_USERS = 11;
    private static final int EXPECTED_WATCHLIST = 7;
    private static final int EXPECTED_FAVOURITES = 4;
    private static final int EXPECTED_WATCH_HISTORY = 9;
    private static final int EXPECTED_CONTINUE_WATCHING = 6;

    private static final String DELIMITER = "|";

    private SeedSqlGenerator() {
    }

    // ------------------------------------------------------------------
    // Kieu du lieu trung gian
    // ------------------------------------------------------------------
    private record Category(long id, String legacyId, String name, String description, boolean active) {
    }

    private record Movie(long id, String legacyId, long categoryId, String title, String director,
            String actors, int releaseYear, String rating, long views, long favouritesCount, boolean active) {
    }

    private record UserRow(long id, String legacyId, String username, String email, String fullName,
            String role, boolean active, List<Long> watchlist, List<Long> favourites,
            List<Long> watchHistory, Map<Long, Integer> continueWatching) {
    }

    private record ProgressRef(long userId, long movieId, int percent) {
    }

    /** Mot dong cua bang trung gian (watchlist / favourites / watch_history). */
    private record Link(long userId, long movieId) {
    }

    // ------------------------------------------------------------------
    // main
    // ------------------------------------------------------------------
    public static void main(String[] args) throws IOException {
        Path dataDir = Paths.get(args.length > 0 ? args[0] : "legacy-cli/data");
        Path outSql = Paths.get(args.length > 1 ? args[1]
                : "backend/src/main/resources/db/migration/V2__seed_data.sql");
        Path outMapping = Paths.get("tools", "build", "seed-mapping.tsv");

        System.out.println("=== CINEVORA SeedSqlGenerator (Phase 1) ===");
        System.out.println("data dir   : " + dataDir.toAbsolutePath());
        System.out.println("out sql    : " + outSql.toAbsolutePath());
        System.out.println();

        // 0) Chan ngay tu dau neu hash demo chua duoc nap dung.
        if (!BCrypt.checkpw(DEMO_PASSWORD, DEMO_PASSWORD_HASH)) {
            throw new IllegalStateException(
                    "DEMO_PASSWORD_HASH khong khop voi DEMO_PASSWORD. Chay PasswordHashGenerator de sinh lai hash.");
        }
        System.out.println("[OK] DEMO_PASSWORD_HASH khop voi mat khau demo (BCrypt.checkpw = true)");

        List<Category> categories = parseCategories(readLines(dataDir.resolve("categories.txt")));
        List<Movie> movies = parseMovies(readLines(dataDir.resolve("movies.txt")), categories);
        List<UserRow> users = parseUsers(readLines(dataDir.resolve("users.txt")), movies);

        List<Link> watchlist = new ArrayList<>();
        List<Link> favourites = new ArrayList<>();
        List<Link> history = new ArrayList<>();
        List<ProgressRef> progress = new ArrayList<>();
        collectRelations(users, watchlist, favourites, history, progress);

        validate(categories, movies, users, watchlist, favourites, history, progress);
        System.out.println("[OK] Du lieu legacy hop le - tat ca kiem tra bat buoc deu PASS");
        System.out.println();

        String sql = buildSql(categories, movies, users, watchlist, favourites, history, progress);
        assertNoPlaintext(sql, readLines(dataDir.resolve("users.txt")));

        writeUtf8NoBom(outSql, sql);
        writeUtf8NoBom(outMapping, buildMappingTsv(categories, movies, users, watchlist, favourites, history, progress));

        printSummary(categories, movies, users, watchlist, favourites, history, progress, outSql, outMapping, sql);
    }

    // ------------------------------------------------------------------
    // Doc + phan tich du lieu legacy
    // ------------------------------------------------------------------

    /** Doc file .txt UTF-8, bo qua dong trong va BOM (neu co). */
    private static List<String> readLines(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("Khong tim thay file du lieu: " + file.toAbsolutePath());
        }
        List<String> lines = new ArrayList<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw;
            if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                line = line.substring(1);
            }
            if (line.isBlank()) {
                continue;
            }
            lines.add(line);
        }
        return lines;
    }

    /**
     * Cat dong theo '|' va bat buoc dung so cot mong doi.
     * Dung limit -1 de giu ca cac cot rong o cuoi (users.txt co dong ket thuc
     * bang nhieu dau '|' lien tiep).
     */
    private static String[] splitFields(String line, int expectedFields, String fileName, int lineNumber) {
        String[] parts = line.split("\\" + DELIMITER, -1);
        if (parts.length != expectedFields) {
            throw new IllegalArgumentException(fileName + " dong " + lineNumber + ": mong doi "
                    + expectedFields + " cot nhung chi co " + parts.length + " cot");
        }
        return parts;
    }

    private static boolean parseBoolean(String value, String where) {
        String v = value.trim();
        if ("true".equalsIgnoreCase(v)) {
            return true;
        }
        if ("false".equalsIgnoreCase(v)) {
            return false;
        }
        throw new IllegalArgumentException(where + ": gia tri boolean khong hop le '" + value + "'");
    }

    /** "CAT01" -> 1, "M27" -> 27. */
    private static long legacyNumericId(String legacyId, String prefix, String where) {
        String id = legacyId.trim();
        if (!id.startsWith(prefix)) {
            throw new IllegalArgumentException(where + ": ma '" + legacyId + "' khong bat dau bang '" + prefix + "'");
        }
        try {
            return Long.parseLong(id.substring(prefix.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(where + ": ma '" + legacyId + "' khong dung dinh dang " + prefix + "nn");
        }
    }

    /**
     * Chuan hoa rating ve dung 1 chu so thap phan theo NUMERIC(3,1).
     * Du lieu legacy la double (co the la "8.4" hoac "8"); neu co 2 chu so thap
     * phan thi file seed se sai kieu -> dung lai ngay.
     */
    private static String normalizeRating(String raw, String movieId) {
        try {
            return new BigDecimal(raw.trim()).setScale(1, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("movies.txt " + movieId + ": rating '" + raw
                    + "' co nhieu hon 1 chu so thap phan, khong khop NUMERIC(3,1)");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("movies.txt " + movieId + ": rating khong phai so: '" + raw + "'");
        }
    }

    private static void requireCount(String fileName, int actual, int expected) {
        if (actual != expected) {
            throw new IllegalArgumentException(fileName + " phai co " + expected + " dong, thuc te " + actual);
        }
    }

    private static String two(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static long parseNonNegativeLong(String value, String where) {
        try {
            long v = Long.parseLong(value.trim());
            if (v < 0) {
                throw new IllegalArgumentException(where + ": gia tri khong duoc am ('" + value + "')");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(where + ": khong phai so nguyen ('" + value + "')");
        }
    }

    private static int parseReleaseYear(String value, String where) {
        int year;
        try {
            year = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(where + ": nam phat hanh khong phai so ('" + value + "')");
        }
        if (year < 1888) {
            throw new IllegalArgumentException(where + ": nam phat hanh phai >= 1888 ('" + value + "')");
        }
        return year;
    }

    /** categories.txt : id|name|description|isActive */
    private static List<Category> parseCategories(List<String> lines) {
        requireCount("categories.txt", lines.size(), EXPECTED_CATEGORIES);
        List<Category> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String where = "categories.txt dong " + lineNo;
            String[] f = splitFields(lines.get(i), 4, "categories.txt", lineNo);
            long id = legacyNumericId(f[0], "CAT", where);
            if (id != lineNo) {
                throw new IllegalArgumentException(where + ": ma '" + f[0].trim()
                        + "' khong khop thu tu dong (mong doi CAT" + two(lineNo) + ")");
            }
            out.add(new Category(id, f[0].trim(), f[1].trim(), f[2].trim(), parseBoolean(f[3], where)));
        }
        return out;
    }

    /**
     * movies.txt : id|title|categoryId|director|actors|releaseYear|rating|views|favouritesCount|isActive
     * Cac cot duration_minutes / video_url / thumbnail_url / description khong
     * co trong du lieu CLI -> de NULL trong V2.
     */
    private static List<Movie> parseMovies(List<String> lines, List<Category> categories) {
        requireCount("movies.txt", lines.size(), EXPECTED_MOVIES);

        Set<Long> categoryIds = new HashSet<>();
        for (Category category : categories) {
            categoryIds.add(category.id());
        }

        List<Movie> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            String where = "movies.txt dong " + lineNo;
            String[] f = splitFields(lines.get(i), 10, "movies.txt", lineNo);

            long id = legacyNumericId(f[0], "M", where);
            if (id != lineNo) {
                throw new IllegalArgumentException(where + ": ma '" + f[0].trim()
                        + "' khong khop thu tu dong (mong doi M" + two(lineNo) + ")");
            }

            long categoryId = legacyNumericId(f[2], "CAT", where + " cot category_id");
            if (!categoryIds.contains(categoryId)) {
                throw new IllegalArgumentException(where + ": the loai '" + f[2].trim()
                        + "' khong ton tai trong categories.txt");
            }

            out.add(new Movie(
                    id,
                    f[0].trim(),
                    categoryId,
                    f[1].trim(),
                    f[3].trim(),
                    f[4].trim(),
                    parseReleaseYear(f[5], where),
                    normalizeRating(f[6], f[0].trim()),
                    parseNonNegativeLong(f[7], where + " cot views"),
                    parseNonNegativeLong(f[8], where + " cot favourites_count"),
                    parseBoolean(f[9], where)));
        }
        return out;
    }

    /**
     * users.txt
     *   ADMIN    : id|username|password|fullName|email|role|isActive
     *   CUSTOMER : id|username|password|fullName|email|role|isActive|watchlist|favourites|watchHistory|continueWatching
     *
     * Cot 3 (mat khau plaintext) duoc BO QUA hoan toan o day; gia tri goc chi
     * duoc doc lai o buoc assertNoPlaintext de chac chan no khong lot vao SQL.
     */
    private static List<UserRow> parseUsers(List<String> lines, List<Movie> movies) {
        requireCount("users.txt", lines.size(), EXPECTED_USERS);

        Set<Long> movieIds = new HashSet<>();
        for (Movie movie : movies) {
            movieIds.add(movie.id());
        }

        List<UserRow> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            long id = lineNo; // id surrogate = so thu tu dong (1-based)
            String where = "users.txt dong " + lineNo;
            String[] f = lines.get(i).split("\\" + DELIMITER, -1);

            if (f.length < 7) {
                throw new IllegalArgumentException(where + ": can toi thieu 7 cot, chi co " + f.length);
            }

            String legacyId = f[0].trim();
            String username = f[1].trim();
            String fullName = f[3].trim();
            String email = f[4].trim();
            String role = f[5].trim().toUpperCase(Locale.ROOT);
            boolean active = parseBoolean(f[6], where);

            if ("ADMIN".equals(role)) {
                if (f.length != 7) {
                    throw new IllegalArgumentException(where
                            + ": ADMIN chi duoc co 7 cot (khong co danh sach phim), thuc te " + f.length);
                }
                out.add(new UserRow(id, legacyId, username, email, fullName, role, active,
                        List.of(), List.of(), List.of(), Map.of()));
                continue;
            }

            if (!"CUSTOMER".equals(role)) {
                throw new IllegalArgumentException(where + ": role '" + role + "' khong hop le (chi ADMIN | CUSTOMER)");
            }
            if (f.length < 10) {
                throw new IllegalArgumentException(where + ": CUSTOMER phai co it nhat 10 cot, chi co " + f.length);
            }

            List<Long> watchlist = parseMovieRefList(f[7], movieIds, where + " cot watchlist", false);
            List<Long> favourites = parseMovieRefList(f[8], movieIds, where + " cot favourites", false);
            // watch_history CHO PHEP trung: cung 1 phim co the duoc xem lai nhieu lan.
            List<Long> watchHistory = parseMovieRefList(f[9], movieIds, where + " cot watchHistory", true);
            Map<Long, Integer> continueWatching = f.length >= 11
                    ? parseProgressList(f[10], movieIds, where + " cot continueWatching")
                    : Map.of();

            out.add(new UserRow(id, legacyId, username, email, fullName, role, active,
                    watchlist, favourites, watchHistory, continueWatching));
        }
        return out;
    }

    /**
     * "M27,M33" -> [27, 33]. Chan phim khong ton tai trong movies.txt.
     *
     * allowDuplicates = false (watchlist, favourites): mot phim chi duoc xuat
     * hien 1 lan vi bang co khoa chinh ghep (user_id, movie_id).
     * allowDuplicates = true (watch_history): nguoi dung co the xem lai cung
     * mot phim nhieu lan - du lieu that cua leo10 co M01 xuat hien 2 lan, va
     * do chinh la ly do watch_history phai co khoa chinh rieng (id).
     */
    private static List<Long> parseMovieRefList(String data, Set<Long> movieIds, String where,
            boolean allowDuplicates) {
        List<Long> out = new ArrayList<>();
        String trimmed = data.trim();
        if (trimmed.isEmpty()) {
            return out;
        }
        for (String item : trimmed.split(",")) {
            String token = item.trim();
            if (token.isEmpty()) {
                continue;
            }
            long movieId = legacyNumericId(token, "M", where);
            if (!movieIds.contains(movieId)) {
                throw new IllegalArgumentException(where + ": phim '" + token + "' khong ton tai trong movies.txt");
            }
            if (!allowDuplicates && out.contains(movieId)) {
                throw new IllegalArgumentException(where + ": phim '" + token
                        + "' bi lap lai -> vi pham khoa chinh ghep (user_id, movie_id)");
            }
            out.add(movieId);
        }
        return out;
    }

    /** "M36:45,M08:0" -> {36=45, 8=0}. Chan trung lap va percent ngoai 0..100. */
    private static Map<Long, Integer> parseProgressList(String data, Set<Long> movieIds, String where) {
        Map<Long, Integer> out = new LinkedHashMap<>();
        String trimmed = data.trim();
        if (trimmed.isEmpty()) {
            return out;
        }
        for (String item : trimmed.split(",")) {
            String token = item.trim();
            if (token.isEmpty()) {
                continue;
            }
            int colon = token.indexOf(':');
            if (colon <= 0 || colon == token.length() - 1) {
                throw new IllegalArgumentException(where + ": phan tu '" + token
                        + "' khong dung dinh dang Mnn:percent");
            }
            String movieToken = token.substring(0, colon).trim();
            long movieId = legacyNumericId(movieToken, "M", where);
            if (!movieIds.contains(movieId)) {
                throw new IllegalArgumentException(where + ": phim '" + movieToken
                        + "' khong ton tai trong movies.txt");
            }
            int percent;
            try {
                percent = Integer.parseInt(token.substring(colon + 1).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(where + ": percent khong phai so ('" + token + "')");
            }
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException(where + ": percent phai nam trong 0..100 ('" + token + "')");
            }
            if (out.put(movieId, percent) != null) {
                throw new IllegalArgumentException(where + ": phim '" + movieToken
                        + "' bi lap lai -> vi pham khoa chinh ghep (user_id, movie_id)");
            }
        }
        return out;
    }

    /**
     * Dua cac tham chieu trong UserRow ve 3 danh sach phang + 1 danh sach tien do.
     * Thu tu duoc giu nguyen theo thu tu dong trong users.txt (xac dinh).
     */
    private static void collectRelations(List<UserRow> users,
            List<Link> watchlist, List<Link> favourites, List<Link> history,
            List<ProgressRef> progress) {
        for (UserRow user : users) {
            for (Long movieId : user.watchlist()) {
                watchlist.add(new Link(user.id(), movieId));
            }
            for (Long movieId : user.favourites()) {
                favourites.add(new Link(user.id(), movieId));
            }
            for (Long movieId : user.watchHistory()) {
                history.add(new Link(user.id(), movieId));
            }
            for (Map.Entry<Long, Integer> entry : user.continueWatching().entrySet()) {
                progress.add(new ProgressRef(user.id(), entry.getKey(), entry.getValue()));
            }
        }
    }

    // ------------------------------------------------------------------
    // Kiem tra tinh toan ven (thay cho smoke test khi chua co database)
    // ------------------------------------------------------------------

    private static void validate(List<Category> categories, List<Movie> movies, List<UserRow> users,
            List<Link> watchlist, List<Link> favourites, List<Link> history, List<ProgressRef> progress) {

        // (1) So dong phai khop voi du lieu legacy da kiem chung bang tay.
        checkCount("categories", categories.size(), EXPECTED_CATEGORIES);
        checkCount("movies", movies.size(), EXPECTED_MOVIES);
        checkCount("users", users.size(), EXPECTED_USERS);
        checkCount("watchlist", watchlist.size(), EXPECTED_WATCHLIST);
        checkCount("favourites", favourites.size(), EXPECTED_FAVOURITES);
        checkCount("watch_history", history.size(), EXPECTED_WATCH_HISTORY);
        checkCount("continue_watching", progress.size(), EXPECTED_CONTINUE_WATCHING);

        // (2) ID surrogate phai lien tuc 1..n (dung cho ALTER ... RESTART WITH).
        for (int i = 0; i < categories.size(); i++) {
            requireSequential("categories", categories.get(i).id(), i + 1);
        }
        for (int i = 0; i < movies.size(); i++) {
            requireSequential("movies", movies.get(i).id(), i + 1);
        }
        for (int i = 0; i < users.size(); i++) {
            requireSequential("users", users.get(i).id(), i + 1);
        }

        // (3) Rang buoc UNIQUE cua bang categories.
        Set<String> categoryNames = new HashSet<>();
        for (Category category : categories) {
            if (!categoryNames.add(category.name().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("categories.name bi trung: '" + category.name() + "'");
            }
        }

        // (4) UNIQUE username/email (username so sanh khong phan biet hoa/thuong).
        Set<String> usernames = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (UserRow user : users) {
            if (user.username().isEmpty() || user.email().isEmpty() || user.fullName().isEmpty()) {
                throw new IllegalArgumentException("users.txt dong " + user.id()
                        + ": username/email/full_name khong duoc rong");
            }
            if (!usernames.add(user.username().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        "username bi trung (khong phan biet hoa/thuong): '" + user.username() + "'");
            }
            if (!emails.add(user.email().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        "email bi trung (khong phan biet hoa/thuong): '" + user.email() + "'");
            }
        }

        // (5) Dung 1 ADMIN; ADMIN khong duoc co danh sach phim (giong legacy).
        int adminCount = 0;
        for (UserRow user : users) {
            if (!"ADMIN".equals(user.role())) {
                continue;
            }
            adminCount++;
            if (!user.watchlist().isEmpty() || !user.favourites().isEmpty()
                    || !user.watchHistory().isEmpty() || !user.continueWatching().isEmpty()) {
                throw new IllegalArgumentException("users.txt dong " + user.id()
                        + ": ADMIN khong duoc co watchlist/favourites/watch_history/continue_watching");
            }
        }
        if (adminCount != 1) {
            throw new IllegalArgumentException("Phai co dung 1 tai khoan ADMIN, thuc te " + adminCount);
        }

        // (6) Toan ven khoa ngoai + khong trung khoa chinh ghep.
        Set<Long> userIds = new HashSet<>();
        for (UserRow user : users) {
            userIds.add(user.id());
        }
        Set<Long> movieIds = new HashSet<>();
        for (Movie movie : movies) {
            movieIds.add(movie.id());
        }
        checkLinks("watchlist", watchlist, userIds, movieIds, false);
        checkLinks("favourites", favourites, userIds, movieIds, false);
        checkLinks("watch_history", history, userIds, movieIds, true);

        Set<String> progressKeys = new HashSet<>();
        for (ProgressRef ref : progress) {
            if (!userIds.contains(ref.userId())) {
                throw new IllegalArgumentException("continue_watching: user_id " + ref.userId() + " khong ton tai");
            }
            if (!movieIds.contains(ref.movieId())) {
                throw new IllegalArgumentException("continue_watching: movie_id " + ref.movieId() + " khong ton tai");
            }
            if (ref.percent() < 0 || ref.percent() > 100) {
                throw new IllegalArgumentException("continue_watching: percent ngoai 0..100 (" + ref.percent() + ")");
            }
            if (!progressKeys.add(ref.userId() + ":" + ref.movieId())) {
                throw new IllegalArgumentException("continue_watching: trung khoa chinh ghep ("
                        + ref.userId() + ", " + ref.movieId() + ")");
            }
        }
    }

    private static void checkCount(String table, int actual, int expected) {
        if (actual != expected) {
            throw new IllegalArgumentException("Bang " + table + ": mong doi " + expected
                    + " dong, thuc te " + actual + " dong");
        }
    }

    private static void requireSequential(String table, long id, long expected) {
        if (id != expected) {
            throw new IllegalArgumentException("Bang " + table + ": id surrogate phai lien tuc tu 1. Mong doi "
                    + expected + " nhung nhan duoc " + id);
        }
    }

    private static void checkLinks(String table, List<Link> links, Set<Long> userIds, Set<Long> movieIds,
            boolean allowDuplicates) {
        Set<String> keys = new HashSet<>();
        for (Link link : links) {
            if (!userIds.contains(link.userId())) {
                throw new IllegalArgumentException(table + ": user_id " + link.userId() + " khong ton tai");
            }
            if (!movieIds.contains(link.movieId())) {
                throw new IllegalArgumentException(table + ": movie_id " + link.movieId() + " khong ton tai");
            }
            if (!allowDuplicates && !keys.add(link.userId() + ":" + link.movieId())) {
                throw new IllegalArgumentException(table + ": trung khoa chinh ghep ("
                        + link.userId() + ", " + link.movieId() + ")");
            }
        }
    }

    /**
     * Chan cuoi cung ve bao mat: quet file SQL vua sinh, dam bao KHONG chua bat
     * ky mat khau plaintext nao cua users.txt (cot 3).
     */
    private static void assertNoPlaintext(String sql, List<String> userLines) {
        int checked = 0;
        for (String line : userLines) {
            String[] f = line.split("\\" + DELIMITER, -1);
            if (f.length < 3) {
                continue;
            }
            String plainPassword = f[2].trim();
            if (plainPassword.isEmpty()) {
                continue;
            }
            checked++;
            if (sql.contains(plainPassword)) {
                throw new IllegalStateException("PHAT HIEN MAT KHAU PLAINTEXT '" + plainPassword
                        + "' trong file SQL sinh ra. Da huy viec ghi file.");
            }
        }
        if (checked != EXPECTED_USERS) {
            throw new IllegalStateException("Chi kiem tra duoc " + checked + "/" + EXPECTED_USERS
                    + " mat khau legacy - khong the ket luan file SQL sach.");
        }
        System.out.println("[OK] Quet bao mat: " + checked + "/" + EXPECTED_USERS
                + " mat khau plaintext cua users.txt deu KHONG xuat hien trong SQL");
    }

    // ------------------------------------------------------------------
    // Sinh SQL
    // ------------------------------------------------------------------

    /** Thoi diem seed = EPOCH + dayOffset ngay + rowIndex * 17 phut (xac dinh). */
    private static String stamp(int dayOffset, int rowIndex) {
        return EPOCH.plusDays(dayOffset).plusMinutes(rowIndex * 17L).format(TS_FORMAT);
    }

    /** Boc chuoi SQL: ' -> '' . Day la file tinh nen escape thu cong la du an toan. */
    private static String q(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    /** Sinh 1 cau INSERT ... VALUES nhieu dong (nhanh hon nhieu so voi n cau). */
    private static void appendInsert(StringBuilder sb, String table, String columns, List<String> rows) {
        sb.append("INSERT INTO ").append(table).append(" (").append(columns).append(") VALUES\n");
        for (int i = 0; i < rows.size(); i++) {
            sb.append("    ").append(rows.get(i));
            sb.append(i == rows.size() - 1 ? ";\n\n" : ",\n");
        }
    }

    private static String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM khong ho tro SHA-256", e);
        }
    }

    private static String buildSql(List<Category> categories, List<Movie> movies, List<UserRow> users,
            List<Link> watchlist, List<Link> favourites, List<Link> history, List<ProgressRef> progress) {

        StringBuilder sb = new StringBuilder(96 * 1024);
        sb.append(buildHeader(categories, movies, users, watchlist, favourites, history, progress));

        List<String> rows = new ArrayList<>();

        // (1) categories
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            rows.add("(" + c.id() + ", " + q(c.name()) + ", " + q(c.description()) + ", " + c.active()
                    + ", " + q(stamp(1, i)) + ", " + q(stamp(1, i)) + ")");
        }
        appendInsert(sb, "categories", "id, name, description, is_active, created_at, updated_at", rows);

        // (2) movies
        rows.clear();
        for (int i = 0; i < movies.size(); i++) {
            Movie m = movies.get(i);
            rows.add("(" + m.id() + ", " + m.categoryId() + ", " + q(m.title()) + ", " + q(m.director())
                    + ", " + q(m.actors()) + ", " + m.releaseYear() + ", " + m.rating() + ", "
                    + m.views() + ", " + m.favouritesCount() + ", " + m.active()
                    + ", " + q(stamp(2, i)) + ", " + q(stamp(2, i)) + ")");
        }
        appendInsert(sb, "movies",
                "id, category_id, title, director, actors, release_year, rating, views, "
                        + "favourites_count, is_active, created_at, updated_at",
                rows);

        // (3) users - moi tai khoan dung chung 1 BCrypt hash cua mat khau demo
        rows.clear();
        for (int i = 0; i < users.size(); i++) {
            UserRow u = users.get(i);
            rows.add("(" + u.id() + ", " + q(u.username()) + ", " + q(u.email()) + ", "
                    + q(DEMO_PASSWORD_HASH) + ", " + q(u.fullName()) + ", " + q(u.role()) + ", "
                    + u.active() + ", " + q(stamp(3, i)) + ", " + q(stamp(3, i)) + ")");
        }
        appendInsert(sb, "users",
                "id, username, email, password, full_name, role, is_active, created_at, updated_at", rows);

        // (4) watchlist
        rows.clear();
        for (int i = 0; i < watchlist.size(); i++) {
            Link link = watchlist.get(i);
            rows.add("(" + link.userId() + ", " + link.movieId() + ", " + q(stamp(4, i)) + ")");
        }
        appendInsert(sb, "watchlist", "user_id, movie_id, added_at", rows);

        // (5) favourites
        rows.clear();
        for (int i = 0; i < favourites.size(); i++) {
            Link link = favourites.get(i);
            rows.add("(" + link.userId() + ", " + link.movieId() + ", " + q(stamp(5, i)) + ")");
        }
        appendInsert(sb, "favourites", "user_id, movie_id, added_at", rows);

        // (6) watch_history
        rows.clear();
        for (int i = 0; i < history.size(); i++) {
            Link link = history.get(i);
            rows.add("(" + link.userId() + ", " + link.movieId() + ", " + q(stamp(6, i)) + ")");
        }
        appendInsert(sb, "watch_history", "user_id, movie_id, watched_at", rows);

        // (7) continue_watching
        rows.clear();
        for (int i = 0; i < progress.size(); i++) {
            ProgressRef ref = progress.get(i);
            rows.add("(" + ref.userId() + ", " + ref.movieId() + ", " + ref.percent()
                    + ", " + q(stamp(7, i)) + ")");
        }
        appendInsert(sb, "continue_watching", "user_id, movie_id, percent, updated_at", rows);

        sb.append(buildSequenceReset(categories, movies, users, history));
        return sb.toString();
    }

    /** Phan tieu de: nguon goc file + quy mo seed. */
    private static String buildHeader(List<Category> categories, List<Movie> movies, List<UserRow> users,
            List<Link> watchlist, List<Link> favourites, List<Link> history, List<ProgressRef> progress) {

        int total = categories.size() + movies.size() + users.size()
                + watchlist.size() + favourites.size() + history.size() + progress.size();

        StringBuilder h = new StringBuilder(4096);
        h.append("-- =====================================================================\n");
        h.append("-- CINEVORA - Migration V2 : Seed data (du lieu mau tu ban CLI cu)\n");
        h.append("-- =====================================================================\n");
        h.append("-- FILE NAY DUOC SINH TU DONG - KHONG SUA TAY.\n");
        h.append("--   Cong cu sinh : tools/SeedSqlGenerator.java (chi can JDK, khong can Maven)\n");
        h.append("--   Nguon du lieu: legacy-cli/data/{categories,movies,users}.txt\n");
        h.append("--   Tai lieu     : docs/database/seed-mapping.md\n");
        h.append("--\n");
        h.append("-- BANG ANH XA ID (xac dinh, 1-1):\n");
        h.append("--   CATnn (categories.txt) -> n\n");
        h.append("--   Mnn   (movies.txt)     -> n\n");
        h.append("--   users.txt              -> so thu tu dong, 1-based\n");
        h.append("--\n");
        h.append("-- BAO MAT: cot users.password KHONG lay tu CLI (CLI luu plaintext).\n");
        h.append("-- Tat ca tai khoan dung chung 1 BCrypt hash (cost 10) cua mat khau demo.\n");
        h.append("-- Mat khau demo nam trong .env.example va seed-mapping.md, KHONG ghi o day.\n");
        h.append("--\n");
        h.append("-- THOI DIEM: gia lap, xac dinh. Moc goc ").append(EPOCH.format(TS_FORMAT)).append("\n");
        h.append("-- 7 bang lan luot lech nhau 1 ngay (categories = moc goc +1 ngay,\n");
        h.append("-- movies = +2, users = +3, ... continue_watching = +7); trong moi bang\n");
        h.append("-- cac dong lech nhau 17 phut. updated_at = created_at.\n");
        h.append("--\n");
        h.append("-- QUY MO SEED:\n");
        h.append("--   categories          = ").append(categories.size()).append("\n");
        h.append("--   movies              = ").append(movies.size()).append("\n");
        h.append("--   users               = ").append(users.size()).append("\n");
        h.append("--   watchlist           = ").append(watchlist.size()).append("\n");
        h.append("--   favourites          = ").append(favourites.size()).append("\n");
        h.append("--   watch_history       = ").append(history.size()).append("\n");
        h.append("--   continue_watching   = ").append(progress.size()).append("\n");
        h.append("--   TONG                = ").append(total).append(" dong\n");
        h.append("-- =====================================================================\n\n");
        return h.toString();
    }

    /**
     * Dong bo identity sequence sau khi seed bang id tuong minh.
     * Bo qua buoc nay se lam Phase 2 loi "duplicate key" ngay lan INSERT dau tien.
     */
    private static String buildSequenceReset(List<Category> categories, List<Movie> movies,
            List<UserRow> users, List<Link> history) {

        StringBuilder r = new StringBuilder(2048);
        r.append("-- =====================================================================\n");
        r.append("-- DONG BO IDENTITY SEQUENCE (BAT BUOC)\n");
        r.append("-- ---------------------------------------------------------------------\n");
        r.append("-- V1 khai bao id la GENERATED BY DEFAULT AS IDENTITY de V2 co the chen id\n");
        r.append("-- tuong minh 1..n. PostgreSQL KHONG tu tang sequence khi id duoc chen thu\n");
        r.append("-- cong, nen sequence van o 1 -> lan INSERT tiep theo se bao:\n");
        r.append("--   ERROR: duplicate key value violates unique constraint \"pk_users\"\n");
        r.append("-- RESTART WITH n = gia tri KE TIEP duoc cap phat = max(id) + 1.\n");
        r.append("-- =====================================================================\n");
        r.append("ALTER TABLE categories    ALTER COLUMN id RESTART WITH ").append(categories.size() + 1).append(";\n");
        r.append("ALTER TABLE movies        ALTER COLUMN id RESTART WITH ").append(movies.size() + 1).append(";\n");
        r.append("ALTER TABLE users         ALTER COLUMN id RESTART WITH ").append(users.size() + 1).append(";\n");
        r.append("ALTER TABLE watch_history ALTER COLUMN id RESTART WITH ").append(history.size() + 1).append(";\n");
        r.append("\n-- Kiem tra nhanh sau khi migrate:\n");
        r.append("--   SELECT count(*) FROM users;   -- mong doi 11\n");
        r.append("--   SELECT max(id)  FROM users;   -- mong doi 11\n");
        r.append("--   INSERT INTO users (username, email, password, full_name, role)\n");
        r.append("--       VALUES ('probe', 'probe@local.test', 'x', 'Probe', 'CUSTOMER'); -- id = 12\n");
        r.append("-- =====================================================================\n");
        return r.toString();
    }

    // ------------------------------------------------------------------
    // Ghi file + bao cao
    // ------------------------------------------------------------------

    /** Bang anh xa ID dang TSV de doi chieu nhanh voi docs/database/seed-mapping.md. */
    private static String buildMappingTsv(List<Category> categories, List<Movie> movies, List<UserRow> users,
            List<Link> watchlist, List<Link> favourites, List<Link> history, List<ProgressRef> progress) {

        StringBuilder sb = new StringBuilder(16384);
        sb.append("table\tlegacy_id\tsurrogate_id\tnote\n");
        for (Category category : categories) {
            sb.append("categories\t").append(category.legacyId()).append("\t").append(category.id())
                    .append("\t").append(category.name()).append("\n");
        }
        for (Movie movie : movies) {
            sb.append("movies\t").append(movie.legacyId()).append("\t").append(movie.id())
                    .append("\tcategory_id=").append(movie.categoryId()).append("\n");
        }
        for (UserRow user : users) {
            sb.append("users\t").append(user.legacyId()).append("\t").append(user.id())
                    .append("\t").append(user.role()).append(" username=").append(user.username()).append("\n");
        }
        sb.append("summary\twatchlist\t").append(watchlist.size()).append("\t\n");
        sb.append("summary\tfavourites\t").append(favourites.size()).append("\t\n");
        sb.append("summary\twatch_history\t").append(history.size()).append("\t\n");
        sb.append("summary\tcontinue_watching\t").append(progress.size()).append("\t\n");
        sb.append("summary\tTOTAL\t")
                .append(categories.size() + movies.size() + users.size()
                        + watchlist.size() + favourites.size() + history.size() + progress.size())
                .append("\t\n");
        return sb.toString();
    }

    /** Ghi file UTF-8 KHONG BOM (Flyway/psql khong chap nhan BOM o dau file SQL). */
    private static void writeUtf8NoBom(Path file, String content) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);

        System.out.println("Da ghi : " + file.toAbsolutePath());
        System.out.println("  bytes   : " + bytes.length);
        System.out.println("  lines   : " + content.split("\n", -1).length);
        System.out.println("  sha256  : " + sha256(bytes));
    }

    private static void printSummary(List<Category> categories, List<Movie> movies, List<UserRow> users,
            List<Link> watchlist, List<Link> favourites, List<Link> history, List<ProgressRef> progress,
            Path outSql, Path outMapping, String sql) {

        int total = categories.size() + movies.size() + users.size()
                + watchlist.size() + favourites.size() + history.size() + progress.size();

        System.out.println();
        System.out.println("=== TONG KET SEED ===");
        System.out.println("categories        : " + categories.size() + "  (mong doi " + EXPECTED_CATEGORIES + ")");
        System.out.println("movies            : " + movies.size() + "  (mong doi " + EXPECTED_MOVIES + ")");
        System.out.println("users             : " + users.size() + "  (mong doi " + EXPECTED_USERS + ")");
        System.out.println("watchlist         : " + watchlist.size() + "  (mong doi " + EXPECTED_WATCHLIST + ")");
        System.out.println("favourites        : " + favourites.size() + "  (mong doi " + EXPECTED_FAVOURITES + ")");
        System.out.println("watch_history     : " + history.size() + "  (mong doi " + EXPECTED_WATCH_HISTORY + ")");
        System.out.println("continue_watching : " + progress.size() + "  (mong doi " + EXPECTED_CONTINUE_WATCHING + ")");
        System.out.println("TONG              : " + total + " dong du lieu");
        System.out.println("So cau INSERT     : " + countOccurrences(sql, "\nINSERT INTO "));
        System.out.println();
        System.out.println("Identity sequence sau seed: categories=8, movies=61, users=12, watch_history=10");
        System.out.println();
        System.out.println("KET QUA: THANH CONG -> " + outSql + " | " + outMapping);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while ((from = haystack.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
