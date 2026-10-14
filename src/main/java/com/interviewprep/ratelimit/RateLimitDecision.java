package com.interviewprep.ratelimit;

public record RateLimitDecision(boolean allowed, int remaining, long retryAfterSeconds) {

    static RateLimitDecision allow(int remaining) {
        return new RateLimitDecision(true, remaining, 0);
    }

    static RateLimitDecision reject(long retryAfterSeconds) {
        return new RateLimitDecision(false, 0, retryAfterSeconds);
    }
}
