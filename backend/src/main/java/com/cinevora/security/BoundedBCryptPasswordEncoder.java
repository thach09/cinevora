package com.cinevora.security;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.charset.StandardCharsets;
public class BoundedBCryptPasswordEncoder extends BCryptPasswordEncoder {
    @Override public String encode(CharSequence raw) {
        if (raw == null || raw.toString().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new com.cinevora.exception.BusinessException("Password must be at most 72 UTF-8 bytes");
        return super.encode(raw);
    }
    @Override public boolean matches(CharSequence raw, String encoded) {
        return raw != null && raw.toString().getBytes(StandardCharsets.UTF_8).length <= 72 && super.matches(raw, encoded);
    }
}
