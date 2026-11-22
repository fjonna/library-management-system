package com.fjonna.library.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TokenBucketTest {

    @Test
    void allowsUpToCapacityThenDenies() {
        TokenBucket bucket = new TokenBucket(3, Duration.ofMinutes(1));

        assertThat(bucket.tryConsume().allowed()).isTrue();
        assertThat(bucket.tryConsume().allowed()).isTrue();
        assertThat(bucket.tryConsume().allowed()).isTrue();

        TokenBucket.Result fourth = bucket.tryConsume();
        assertThat(fourth.allowed()).isFalse();
        assertThat(fourth.retryAfterSeconds()).isPositive();
    }

    @Test
    void independentBucketsDoNotShareTokens() {
        TokenBucket a = new TokenBucket(1, Duration.ofMinutes(1));
        TokenBucket b = new TokenBucket(1, Duration.ofMinutes(1));

        assertThat(a.tryConsume().allowed()).isTrue();

        assertThat(b.tryConsume().allowed()).isTrue();
    }
}
