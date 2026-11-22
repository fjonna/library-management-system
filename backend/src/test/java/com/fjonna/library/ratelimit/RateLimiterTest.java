package com.fjonna.library.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final RateLimiter rateLimiter = new RateLimiter();

    @Test
    void sameActionAndKeyShareOneBucket() {
        assertThat(rateLimiter.tryConsume("agent-query", "user-1", 2, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(rateLimiter.tryConsume("agent-query", "user-1", 2, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(rateLimiter.tryConsume("agent-query", "user-1", 2, Duration.ofMinutes(1)).allowed()).isFalse();
    }

    @Test
    void differentUsersGetIndependentLimits() {
        assertThat(rateLimiter.tryConsume("agent-query", "user-2", 1, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(rateLimiter.tryConsume("agent-query", "user-2", 1, Duration.ofMinutes(1)).allowed()).isFalse();

        assertThat(rateLimiter.tryConsume("agent-query", "user-3", 1, Duration.ofMinutes(1)).allowed()).isTrue();
    }

    @Test
    void differentActionsForSameKeyAreIndependent() {
        assertThat(rateLimiter.tryConsume("agent-query", "user-4", 1, Duration.ofMinutes(1)).allowed()).isTrue();
        assertThat(rateLimiter.tryConsume("agent-query", "user-4", 1, Duration.ofMinutes(1)).allowed()).isFalse();

        assertThat(rateLimiter.tryConsume("some-other-action", "user-4", 1, Duration.ofMinutes(1)).allowed()).isTrue();
    }
}
