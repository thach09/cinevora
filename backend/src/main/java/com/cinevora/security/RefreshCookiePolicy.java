package com.cinevora.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

/** Host-only cookie: deliberately never sets Domain. */
@Component
public class RefreshCookiePolicy {
    public static final String NAME = "cinevora_refresh";
    private final boolean secure;
    private final long days;
    public RefreshCookiePolicy(@Value("${app.auth.cookie-secure:false}") boolean secure,
                               @Value("${app.auth.refresh-days:30}") long days) {
        this.secure = secure; this.days = days;
    }
    public void write(HttpServletResponse response, String value) {
        response.addHeader("Set-Cookie", ResponseCookie.from(NAME, value == null ? "" : value)
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api/v1/auth")
                .maxAge(value == null ? Duration.ZERO : Duration.ofDays(days)).build().toString());
    }
}
