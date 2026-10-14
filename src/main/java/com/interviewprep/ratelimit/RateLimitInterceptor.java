package com.interviewprep.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final SlidingWindowRateLimiter limiter;

    public RateLimitInterceptor(SlidingWindowRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            throw new MissingApiKeyException(API_KEY_HEADER);
        }
        RateLimitDecision decision = limiter.tryAcquire(apiKey.strip());
        response.setHeader(LIMIT_HEADER, String.valueOf(limiter.maxRequests()));
        response.setHeader(REMAINING_HEADER, String.valueOf(decision.remaining()));
        if (decision.allowed()) {
            return true;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        return false;
    }
}
