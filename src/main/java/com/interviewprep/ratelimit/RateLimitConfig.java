package com.interviewprep.ratelimit;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitProperties properties;
    private final Clock clock;

    public RateLimitConfig(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Bean
    public SlidingWindowRateLimiter slidingWindowRateLimiter() {
        return new SlidingWindowRateLimiter(properties.maxRequests(), properties.window(), clock);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(slidingWindowRateLimiter()))
                .addPathPatterns(properties.paths());
    }
}
