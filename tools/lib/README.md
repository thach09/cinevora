# tools/lib - Thu vien vendor (khong dung Maven/Gradle)

## `src/org/mindrot/jbcrypt/BCrypt.java`

| Thuoc tinh | Gia tri |
|---|---|
| Thu vien | jBCrypt |
| Phien ban | 0.4 |
| Tac gia | Damien Miller (djm@mindrot.org) |
| Giay phep | ISC (xem header trong file - da giu nguyen ban quyen) |
| Nguon | `https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4-sources.jar` |
| Lop Java | `org.mindrot.jbcrypt.BCrypt` |
| So byte | 28019 |
| SHA-256 | `B49DA03BD73772B7979F301E2A0F6CAB899855C48A992FA9ABCA885F9F1C1225` |

### Vi sao vendor thay vi dung Maven?

Phase 1 can sinh **BCrypt hash** cho du lieu seed, nhung:

* Repo chua co `pom.xml`/`build.gradle` (Phase 2 moi khoi tao Spring Boot).
* Yeu cau cua de bai: cong cu ho tro phai chay **offline, chi voi JDK** - khong bat
  nguoi cham bai cai Maven hay truy cap Internet.
* jBCrypt la thu vien **0 dependency**, chi gom 1 file `.java` duy nhat, dung
  thuat toan Blowfish/bcrypt goc giong het OpenBSD `crypt(3)` - nen vendor la
  lua chon gon nhe va minh bach nhat.

### Xac minh lai file (tinh toan ven)

```powershell
(Get-FileHash tools\lib\src\org\mindrot\jbcrypt\BCrypt.java -Algorithm SHA256).Hash
# => B49DA03BD73772B7979F301E2A0F6CAB899855C48A992FA9ABCA885F9F1C1225
```

### Tuong thich voi Spring Security (Phase 2)

Phase 2 dung `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`
(Spring Security ke thua y nguyen thuat toan cua jBCrypt). Hash sinh boi jBCrypt
0.4 co tien to `$2a$` - dung dai ma `BCryptPasswordEncoder.matches()` ho tro san.
Da kiem chung bang mot trinh kiem tra doc lap (Spring Security 6.3.3), xem
`tools/verify/SpringBcryptParityCheck.java` va
`docs/database/migration-runbook.md` muc "Kiem chung BCrypt doc lap".

### Nang cap trong tuong lai

Khi Phase 2 da co Maven, co the bo thu muc nay va thay bang dependency chinh thuc
trong `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

Luu y: KHI DO cac hash `$2a$` cu VAN dung duoc, khong can seed lai.
