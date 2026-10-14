package com.interviewprep.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.shared.error.ProblemTypes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitInterceptor implements HandlerInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final SlidingWindowRateLimiter limiter;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(SlidingWindowRateLimiter limiter, ObjectMapper objectMapper) {
        this.limiter = limiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
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
        writeTooManyRequests(request, response, decision.retryAfterSeconds());
        return false;
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded, retry after " + retryAfterSeconds + " seconds");
        problem.setType(ProblemTypes.of("rate-limit-exceeded"));
        problem.setTitle("Too many requests");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("retryAfterSeconds", retryAfterSeconds);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
