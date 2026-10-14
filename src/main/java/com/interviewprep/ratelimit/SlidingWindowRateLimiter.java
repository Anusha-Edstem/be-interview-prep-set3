package com.interviewprep.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class SlidingWindowRateLimiter {

    private final ConcurrentMap<String, Deque<Instant>> requestLogs = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final Duration window;
    private final Clock clock;

    public SlidingWindowRateLimiter(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.clock = clock;
    }

    public int maxRequests() {
        return maxRequests;
    }

    public RateLimitDecision tryAcquire(String key) {
        Instant now = clock.instant();
        AtomicReference<RateLimitDecision> decision = new AtomicReference<>();
        requestLogs.compute(key, (ignored, existing) -> {
            Deque<Instant> timestamps = existing == null ? new ArrayDeque<>() : existing;
            evictExpired(timestamps, now);
            decision.set(admit(timestamps, now));
            return timestamps;
        });
        return decision.get();
    }

    public void evictIdleKeys() {
        Instant now = clock.instant();
        requestLogs
                .keySet()
                .forEach(key -> requestLogs.computeIfPresent(key, (ignored, timestamps) -> {
                    evictExpired(timestamps, now);
                    return timestamps.isEmpty() ? null : timestamps;
                }));
    }

    int trackedKeyCount() {
        return requestLogs.size();
    }

    private RateLimitDecision admit(Deque<Instant> timestamps, Instant now) {
        if (timestamps.size() >= maxRequests) {
            return RateLimitDecision.reject(secondsUntilSlotFrees(timestamps.peekFirst(), now));
        }
        timestamps.addLast(now);
        return RateLimitDecision.allow(maxRequests - timestamps.size());
    }

    private void evictExpired(Deque<Instant> timestamps, Instant now) {
        Instant cutoff = now.minus(window);
        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
            timestamps.pollFirst();
        }
    }

    private long secondsUntilSlotFrees(Instant oldest, Instant now) {
        Duration wait = Duration.between(now, oldest.plus(window));
        long wholeSeconds = wait.getSeconds() + (wait.getNano() > 0 ? 1 : 0);
        return Math.max(1, wholeSeconds);
    }
}
