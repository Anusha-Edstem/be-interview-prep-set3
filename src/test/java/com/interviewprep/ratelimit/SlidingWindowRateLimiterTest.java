package com.interviewprep.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlidingWindowRateLimiterTest {

    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private MutableClock clock;
    private SlidingWindowRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        limiter = new SlidingWindowRateLimiter(MAX_REQUESTS, WINDOW, clock);
    }

    @Test
    @DisplayName("RL-02 the eleventh request in a minute is rejected")
    void eleventhRequestRejected() {
        List<RateLimitDecision> admitted = acquire("client-a", MAX_REQUESTS);

        RateLimitDecision eleventh = limiter.tryAcquire("client-a");

        assertThat(admitted).allMatch(RateLimitDecision::allowed);
        assertThat(admitted).extracting(RateLimitDecision::remaining).containsExactly(9, 8, 7, 6, 5, 4, 3, 2, 1, 0);
        assertThat(eleventh.allowed()).isFalse();
        assertThat(eleventh.remaining()).isZero();
        assertThat(eleventh.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("RL-02 a request is admitted again once the oldest ones leave the window")
    void windowSlides() {
        acquire("client-a", 5);
        clock.advance(Duration.ofSeconds(30));
        acquire("client-a", 5);
        clock.advance(Duration.ofSeconds(30));

        List<RateLimitDecision> afterFirstBatchExpired = acquire("client-a", 5);
        RateLimitDecision overLimit = limiter.tryAcquire("client-a");

        assertThat(afterFirstBatchExpired).allMatch(RateLimitDecision::allowed);
        assertThat(overLimit.allowed()).isFalse();
        assertThat(overLimit.retryAfterSeconds()).isEqualTo(30);
    }

    @Test
    @DisplayName("RL-02 a request just before the window ends is still rejected")
    void requestJustBeforeWindowEndRejected() {
        acquire("client-a", MAX_REQUESTS);
        clock.advance(WINDOW.minusMillis(1));

        RateLimitDecision decision = limiter.tryAcquire("client-a");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfterSeconds()).isEqualTo(1);
    }

    @Test
    @DisplayName("RL-02 retry-after is rounded up to whole seconds")
    void retryAfterRoundedUp() {
        acquire("client-a", MAX_REQUESTS);
        clock.advance(Duration.ofMillis(500));

        RateLimitDecision decision = limiter.tryAcquire("client-a");

        assertThat(decision.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    @DisplayName("RL-03 requests from different API keys do not affect each other")
    void keysAreIndependent() {
        acquire("client-a", MAX_REQUESTS);

        RateLimitDecision otherKey = limiter.tryAcquire("client-b");
        RateLimitDecision sameKey = limiter.tryAcquire("client-a");

        assertThat(otherKey.allowed()).isTrue();
        assertThat(otherKey.remaining()).isEqualTo(MAX_REQUESTS - 1);
        assertThat(sameKey.allowed()).isFalse();
    }

    @Test
    @DisplayName("RL-05 concurrent requests from one key admit exactly the limit")
    void concurrentRequestsAdmitExactlyTheLimit() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<RateLimitDecision>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            futures.add(executor.submit(() -> {
                startGate.await();
                return limiter.tryAcquire("client-a");
            }));
        }

        startGate.countDown();
        long allowed = 0;
        for (Future<RateLimitDecision> future : futures) {
            allowed += future.get(10, TimeUnit.SECONDS).allowed() ? 1 : 0;
        }
        executor.shutdown();

        assertThat(allowed).isEqualTo(MAX_REQUESTS);
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }

    private List<RateLimitDecision> acquire(String key, int count) {
        return IntStream.range(0, count).mapToObj(i -> limiter.tryAcquire(key)).toList();
    }
}
