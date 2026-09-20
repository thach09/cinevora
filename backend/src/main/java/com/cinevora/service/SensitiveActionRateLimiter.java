package com.cinevora.service;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SensitiveActionRateLimiter {
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allow(String key) {
        Instant now = Instant.now();
        Bucket bucket = buckets.compute(key, (ignored, existing) -> {
            if (existing == null || Duration.between(existing.startedAt(), now).compareTo(WINDOW) >= 0) return new Bucket(now, new AtomicInteger(1));
            existing.count().incrementAndGet();
            return existing;
        });
        return bucket.count().get() <= MAX_ATTEMPTS;
    }

    public void reset(String key) { buckets.remove(key); }
    private record Bucket(Instant startedAt, AtomicInteger count) {}
}
