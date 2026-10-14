package com.interviewprep.ratelimit;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.List;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("rate-limit")
public record RateLimitProperties(
        @Positive int maxRequests,
        @NotNull @DurationMin(seconds = 1) Duration window,
        @NotEmpty List<String> paths) {}
