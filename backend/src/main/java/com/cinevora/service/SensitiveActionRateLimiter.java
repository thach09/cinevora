package com.cinevora.service;

import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/** Fixed-window bounded local counter; replace this adapter before adding replicas. */
@Service
public class SensitiveActionRateLimiter {
    public enum RateClass {
        AUTH_LOGIN_STRICT(30, 60), AUTH_RESET_STRICT(10, 900), AUTH_REGISTER(10, 900), TOKEN_REFRESH(60, 60),
        PUBLIC_SEARCH(120, 60), PUBLIC_READ(300, 60), PERSONALIZED_EXPENSIVE(90, 60), USER_WRITE(120, 60),
        ADMIN_WRITE(120, 60), UPLOAD(20, 60);
        final int maximum; final int seconds;
        RateClass(int maximum, int seconds) { this.maximum = maximum; this.seconds = seconds; }
    }
    private record Bucket(long expires, int count) {}
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final int capacity;
    private final Clock clock;
    public SensitiveActionRateLimiter() { this(10_000, Clock.systemUTC()); }
    SensitiveActionRateLimiter(int capacity, Clock clock) { this.capacity = capacity; this.clock = clock; }
    public synchronized long retryAfter(RateClass policy, String dimension) {
        long now = clock.millis();
        // Scan only bounded resident entries, never retain raw identity strings.
        buckets.values().removeIf(bucket -> bucket.expires <= now);
        String key = policy.name() + ":" + TokenService.sha256(dimension);
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            if (buckets.size() >= capacity) return 1;
            buckets.put(key, new Bucket(now + policy.seconds * 1000L, 1)); return 0;
        }
        if (bucket.count >= policy.maximum) return Math.max(1, (bucket.expires - now + 999) / 1000);
        buckets.put(key, new Bucket(bucket.expires, bucket.count + 1)); return 0;
    }
    public boolean allow(String key) {
        return retryAfter(key.startsWith("reset:") ? RateClass.AUTH_RESET_STRICT : RateClass.AUTH_LOGIN_STRICT, "account:" + key) == 0;
    }
    // Successful login must not reset the IP/account budget and enable credential spraying.
    public void reset(String key) {}
    synchronized int residentKeys() { return buckets.size(); }
}
