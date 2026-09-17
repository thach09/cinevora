import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * CINEVORA - Phase 1 : kiem chung DOC LAP tinh tuong thich BCrypt.
 *
 * VI SAO CAN?
 *   V2__seed_data.sql sinh hash bang jBCrypt 0.4 (vendor trong tools/lib).
 *   Nhung Phase 2 lai xac thuc dang nhap bang
 *   org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.
 *   Neu 2 thu vien khong tuong thich thi tat ca tai khoan seed se KHONG the
 *   dang nhap duoc - loi nay rat kho phat hien muon.
 *
 *   Tool nay doc TRUC TIEP file V2__seed_data.sql, trich ra MOI hash BCrypt,
 *   roi kiem tra bang BCryptPasswordEncoder THAT (khong phai jBCrypt):
 *       - encoder.matches(demoPassword, hash)  phai TRUE voi MOI hash
 *       - encoder.matches(matKhauSai, hash)    phai FALSE
 *       - hash do chinh Spring sinh ra cung phai tu kiem tra duoc
 *
 *   => Chung minh hash trong V2 se dang nhap duoc o Phase 2.
 *
 * YEU CAU: can 2 jar (khong commit vao repo, tai 1 lan tu Maven Central):
 *   tools/build/verify/spring-security-crypto-6.3.3.jar
 *   tools/build/verify/spring-jcl-6.1.12.jar
 *   (chi tiet trong docs/database/migration-runbook.md)
 *
 * CACH CHAY (tu thu muc goc cua repo):
 *   & "$JDK\javac.exe" -encoding UTF-8 -cp "tools\build\verify\*" -d tools\build\verify-classes tools\verify\SpringBcryptParityCheck.java
 *   & "$JDK\java.exe" -cp "tools\build\verify-classes;tools\build\verify\*" SpringBcryptParityCheck
 */
public final class SpringBcryptParityCheck {

    /** Mat khau demo (giong .env.example va docs/database/seed-mapping.md). */
    private static final String DEMO_PASSWORD = "Cinevora@2026";

    /** Bat ky chuoi $2a$ / $2b$ / $2y$ + cost 2 chu so + 53 ky tu base64. */
    private static final Pattern BCRYPT_HASH = Pattern.compile("\\$2[aby]\\$\\d\\d\\$[./A-Za-z0-9]{53}");

    private SpringBcryptParityCheck() {
    }

    public static void main(String[] args) throws IOException {
        Path sqlFile = Paths.get(args.length > 0 ? args[0]
                : "backend/src/main/resources/db/migration/V2__seed_data.sql");

        System.out.println("=== CINEVORA - Spring BCrypt parity check ===");
        System.out.println("file    : " + sqlFile.toAbsolutePath());
        System.out.println("encoder : " + BCryptPasswordEncoder.class.getName());
        System.out.println();

        String sql = Files.readString(sqlFile, StandardCharsets.UTF_8);
        Set<String> hashes = new LinkedHashSet<>();
        Matcher matcher = BCRYPT_HASH.matcher(sql);
        while (matcher.find()) {
            hashes.add(matcher.group());
        }

        if (hashes.isEmpty()) {
            System.err.println("[FAIL] Khong tim thay hash BCrypt nao trong file SQL.");
            System.exit(1);
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        int matched = 0;
        int failed = 0;

        for (String hash : hashes) {
            boolean ok = encoder.matches(DEMO_PASSWORD, hash);
            boolean wrongRejected = !encoder.matches(DEMO_PASSWORD + "x", hash)
                    && !encoder.matches(DEMO_PASSWORD.toUpperCase(), hash);
            System.out.println((ok && wrongRejected ? "  [PASS] " : "  [FAIL] ") + hash
                    + "  matches=" + ok + " wrongRejected=" + wrongRejected);
            if (ok && wrongRejected) {
                matched++;
            } else {
                failed++;
            }
        }

        // Chieu nguoc lai: hash do Spring sinh ra cung phai tu kiem tra duoc
        // (bao dam kieu du lieu/kieu tien to giong nhau).
        String springHash = encoder.encode(DEMO_PASSWORD);
        boolean springRoundTrip = encoder.matches(DEMO_PASSWORD, springHash)
                && springHash.startsWith("$2a$10$");
        System.out.println((springRoundTrip ? "  [PASS] " : "  [FAIL] ")
                + "Spring tu sinh + tu kiem tra: " + springHash);

        // jBCrypt doc duoc hash cua Spring khong? (kiem chieu nguoc lai bang
        // cach chay them 1 lan PasswordHashGenerator --verify trong runbook)
        System.out.println();
        System.out.println("Hash BCrypt duy nhat trong V2 : " + hashes.size());
        System.out.println("So tai khoan seed             : 11");
        System.out.println("Khop voi Spring              : " + matched);
        System.out.println("That bai                     : " + failed);

        if (failed > 0 || !springRoundTrip) {
            System.err.println("KET QUA: FAIL - KHONG duoc dung file seed nay cho Phase 2.");
            System.exit(1);
        }
        System.out.println("KET QUA: PASS - hash trong V2 xac thuc duoc bang BCryptPasswordEncoder");
    }
}
