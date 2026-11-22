package com.fjonna.library.ratelimit;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Per-key in-memory token buckets, one per rate-limited action (e.g. "agent-query"). */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public TokenBucket.Result tryConsume(String action, String key, int capacity, Duration window) {
        TokenBucket bucket = buckets.computeIfAbsent(action + ':' + key, k -> new TokenBucket(capacity, window));
        return bucket.tryConsume();
    }
}
