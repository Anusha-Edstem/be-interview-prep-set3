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
public class RateLimitConfig implements WebMvcConfigurer, SchedulingConfigurer {

    private final RateLimitProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public RateLimitConfig(RateLimitProperties properties, Clock clock, ObjectMapper objectMapper) {
        this.properties = properties;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SlidingWindowRateLimiter slidingWindowRateLimiter() {
        return new SlidingWindowRateLimiter(properties.maxRequests(), properties.window(), clock);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(slidingWindowRateLimiter(), objectMapper))
                .addPathPatterns(properties.paths());
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedDelayTask(slidingWindowRateLimiter()::evictIdleKeys, properties.window());
    }
}
