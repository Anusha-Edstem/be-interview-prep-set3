package com.interviewprep.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public class RateLimitConfig implements SchedulingConfigurer {

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

    @Bean
    public WebMvcConfigurer rateLimitWebMvcConfigurer(SlidingWindowRateLimiter limiter, ObjectMapper objectMapper) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new RateLimitInterceptor(limiter, objectMapper))
                        .addPathPatterns(properties.paths());
            }
        };
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedDelayTask(slidingWindowRateLimiter()::evictIdleKeys, properties.window());
    }
}
