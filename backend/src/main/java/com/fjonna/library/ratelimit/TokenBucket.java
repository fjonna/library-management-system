package com.fjonna.library.ratelimit;

import java.time.Duration;

/** Classic token bucket: {@code capacity} tokens refilled continuously over {@code window}. Thread-safe. */
public class TokenBucket {

    private final int capacity;
    private final double nanosPerToken;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(int capacity, Duration window) {
        this.capacity = capacity;
        this.nanosPerToken = (double) window.toNanos() / capacity;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public record Result(boolean allowed, long retryAfterSeconds) {
    }

    public synchronized Result tryConsume() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return new Result(true, 0);
        }
        long waitNanos = (long) Math.ceil((1 - tokens) * nanosPerToken);
        long retryAfterSeconds = Math.max(1, Duration.ofNanos(waitNanos).toSeconds() + 1);
        return new Result(false, retryAfterSeconds);
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsed = now - lastRefillNanos;
        if (elapsed <= 0) return;
        tokens = Math.min(capacity, tokens + elapsed / nanosPerToken);
        lastRefillNanos = now;
    }
}
