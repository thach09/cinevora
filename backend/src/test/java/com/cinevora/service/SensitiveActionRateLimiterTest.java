package com.cinevora.service;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class SensitiveActionRateLimiterTest {
    @Test void uniqueIdentitySprayCannotExceedCapacity() {
        var limiter = new SensitiveActionRateLimiter(100, Clock.systemUTC());
        for (int i=0; i<10000; i++) limiter.retryAfter(SensitiveActionRateLimiter.RateClass.AUTH_LOGIN_STRICT, "spray"+i);
        assertEquals(100, limiter.residentKeys());
        assertTrue(limiter.retryAfter(SensitiveActionRateLimiter.RateClass.AUTH_LOGIN_STRICT,"new") > 0);
    }
    @Test void burstIsRejectedAndWindowExpires() {
        final long[] now = {0};
        Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return Instant.ofEpochMilli(now[0]); }
        };
        var limiter = new SensitiveActionRateLimiter(100, clock);
        for (int i=0; i<30; i++) assertEquals(0, limiter.retryAfter(SensitiveActionRateLimiter.RateClass.AUTH_LOGIN_STRICT,"client"));
        assertEquals(60, limiter.retryAfter(SensitiveActionRateLimiter.RateClass.AUTH_LOGIN_STRICT,"client"));
        now[0] = 60000;
        assertEquals(0, limiter.retryAfter(SensitiveActionRateLimiter.RateClass.AUTH_LOGIN_STRICT,"client"));
    }
}
