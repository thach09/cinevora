import org.mindrot.jbcrypt.BCrypt;

/**
 * CINEVORA - Phase 1 tool : sinh / kiem tra mat khau bam BCrypt.
 *
 * Muc dich
 *   Sinh 1 hash BCrypt de nhung vao V2__seed_data.sql, dam bao file seed
 *   KHONG BAO GIO chua mat khau plaintext.
 *
 * Khong phu thuoc Maven/Gradle: chi can JDK (>= 17) va jBCrypt da vendor
 * san trong tools/lib/src.
 *
 * Bien dich (PowerShell - thay duong dan JDK 21 cua ban):
 *   $JDK = 'C:\path\to\jdk-21\bin'
 *   & "$JDK\javac" -encoding UTF-8 -d tools\build\classes tools\lib\src\org\mindrot\jbcrypt\BCrypt.java tools\PasswordHashGenerator.java tools\SeedSqlGenerator.java
 *
 * Cach dung
 *   java -cp tools\build\classes PasswordHashGenerator "Cinevora@2026"
 *       -> in ra hash BCrypt (cost 10) + tu kiem tra lai bang BCrypt.checkpw
 *   java -cp tools\build\classes PasswordHashGenerator "Cinevora@2026" 12
 *       -> chi dinh cost (log rounds), mac dinh 10
 *   java -cp tools\build\classes PasswordHashGenerator --verify "Cinevora@2026" "$2a$10$..."
 *       -> kiem tra 1 hash co khop voi mat khau khong (exit code 0 = MATCH)
 *   java -cp tools\build\classes PasswordHashGenerator --self-test
 *       -> round-trip test cho cost 4..12 (chay duoc offline, khong can DB)
 *
 * LUU Y BAO MAT
 *   - Tool nay KHONG BAO GIO in ra mat khau plaintext, chi in hash.
 *   - Mat khau demo nam trong .env.example va docs/database/seed-mapping.md
 *     (day la mat khau demo co chu dich, khong phai secret that).
 */
public final class PasswordHashGenerator {

    /** Cost mac dinh = 10 (2^10 = 1024 vong lap) - dung chuan BCrypt pho bien. */
    private static final int DEFAULT_COST = 10;

    private static final int MIN_COST = 4;
    private static final int MAX_COST = 12;

    private PasswordHashGenerator() {
        // lop tien ich - khong khoi tao
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                usage();
                System.exit(2);
            }

            String mode = args[0];

            if ("--help".equals(mode) || "-h".equals(mode)) {
                usage();
                return;
            }
            if ("--self-test".equals(mode)) {
                selfTest();
                return;
            }
            if ("--verify".equals(mode)) {
                if (args.length != 3) {
                    usage();
                    System.exit(2);
                }
                verify(args[1], args[2]);
                return;
            }

            // Che do mac dinh: sinh hash moi
            generate(args);
        } catch (Exception e) {
            System.err.println("[LOI] " + e.getMessage());
            System.exit(1);
        }
    }

    /** Sinh hash moi cho mat khau truyen vao. */
    private static void generate(String[] args) {
        String plainPassword = args[0];
        if (plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Mat khau khong duoc rong.");
        }

        int cost = DEFAULT_COST;
        if (args.length > 1) {
            cost = Integer.parseInt(args[1]);
        }
        if (cost < MIN_COST || cost > MAX_COST) {
            throw new IllegalArgumentException(
                    "cost phai nam trong khoang " + MIN_COST + ".." + MAX_COST + " (nhan duoc: " + cost + ")");
        }

        String salt = BCrypt.gensalt(cost);
        String hash = BCrypt.hashpw(plainPassword, salt);

        // Tu kiem tra: hash vua sinh PHAI khop lai voi mat khau goc.
        boolean selfCheckOk = BCrypt.checkpw(plainPassword, hash);

        System.out.println("cost            : " + cost);
        System.out.println("hash            : " + hash);
        System.out.println("hash length     : " + hash.length() + " (BCrypt luon = 60)");
        System.out.println("self-check      : " + (selfCheckOk ? "PASS" : "FAIL"));

        if (!selfCheckOk) {
            throw new IllegalStateException("BCrypt.checkpw that bai tren hash vua sinh - thu vien co van de!");
        }
        System.out.println("Ket qua         : dung hash nay cho DEMO_PASSWORD_HASH trong SeedSqlGenerator.java");
    }

    /** Kiem tra 1 hash co khop voi mat khau khong. */
    private static void verify(String plainPassword, String hash) {
        String normalized = normalize2a(hash);
        if (normalized == null) {
            System.err.println("NO-MATCH        : hash khong dung dinh dang BCrypt ($2a$/$2b$/$2y$, dai 60 ky tu)");
            System.exit(1);
        }
        boolean match;
        try {
            match = BCrypt.checkpw(plainPassword, normalized);
        } catch (IllegalArgumentException e) {
            System.err.println("NO-MATCH        : hash khong hop le - " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.println("hash            : " + normalized);
        System.out.println("ket qua         : " + (match ? "MATCH" : "NO-MATCH"));
        System.exit(match ? 0 : 1);
    }

    /**
     * jBCrypt chi hieu tien to $2a$ (va $2$). Spring Security sinh $2a$ theo mac
     * dinh, nhung hash tu he thong khac co the la $2b$/$2y$ (cung thuat toan,
     * chi khac quy uoc tien to) -> chuan hoa ve $2a$ truoc khi kiem tra.
     */
    private static String normalize2a(String hash) {
        if (hash == null) {
            return null;
        }
        String trimmed = hash.trim();
        if (trimmed.length() != 60) {
            return null;
        }
        if (trimmed.startsWith("$2a$")) {
            return trimmed;
        }
        if (trimmed.startsWith("$2b$") || trimmed.startsWith("$2y$")) {
            return "$2a$" + trimmed.substring(4);
        }
        return null;
    }

    /** Round-trip test: hash roi kiem tra lai cho tung muc cost. */
    private static void selfTest() {
        String[] samples = { "Cinevora@2026", "a", "", "Mat khau co dau tieng Viet: Do Thiet Thach" };
        int passed = 0;
        int failed = 0;

        for (int cost = MIN_COST; cost <= MAX_COST; cost++) {
            for (String sample : samples) {
                String hash = BCrypt.hashpw(sample, BCrypt.gensalt(cost));
                boolean ok = hash.length() == 60
                        && BCrypt.checkpw(sample, hash)
                        && hash.startsWith("$2a$" + (cost < 10 ? "0" + cost : cost) + "$");
                if (ok) {
                    passed++;
                } else {
                    failed++;
                    System.err.println("  FAIL cost=" + cost + " sample=\"" + sample + "\"");
                }
            }
        }

        // Mat khau sai KHONG duoc khop (kiem tra am)
        String hash = BCrypt.hashpw("Cinevora@2026", BCrypt.gensalt(DEFAULT_COST));
        boolean negativeOk = !BCrypt.checkpw("cinevora@2026", hash)
                && !BCrypt.checkpw("Cinevora@2027", hash)
                && !BCrypt.checkpw("", hash);

        System.out.println("--- PasswordHashGenerator --self-test ---");
        System.out.println("round-trip     : " + passed + " PASS / " + failed + " FAIL");
        System.out.println("negative test  : " + (negativeOk ? "PASS" : "FAIL")
                + " (mat khau sai phai KHONG khop)");

        if (failed > 0 || !negativeOk) {
            System.err.println("KET QUA: FAIL");
            System.exit(1);
        }
        System.out.println("KET QUA: PASS (" + (passed + 1) + " phep kiem tra)");
    }

    private static void usage() {
        System.out.println("CINEVORA - PasswordHashGenerator (Phase 1)");
        System.out.println();
        System.out.println("  java -cp tools\\build\\classes PasswordHashGenerator <mat-khau> [cost]");
        System.out.println("  java -cp tools\\build\\classes PasswordHashGenerator --verify <mat-khau> <hash>");
        System.out.println("  java -cp tools\\build\\classes PasswordHashGenerator --self-test");
        System.out.println();
        System.out.println("  cost: " + MIN_COST + ".." + MAX_COST + " (mac dinh " + DEFAULT_COST + ")");
    }
}

