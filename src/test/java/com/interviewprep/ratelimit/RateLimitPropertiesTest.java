package com.interviewprep.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RateLimitPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    @DisplayName("RL-06 limit, window and paths bind from configuration")
    void bindsConfiguredValues() {
        runner.withPropertyValues("rate-limit.max-requests=25", "rate-limit.window=2m", "rate-limit.paths=/a/**,/b/**")
                .run(context -> {
                    RateLimitProperties properties = context.getBean(RateLimitProperties.class);

                    assertThat(properties.maxRequests()).isEqualTo(25);
                    assertThat(properties.window()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(properties.paths()).containsExactly("/a/**", "/b/**");
                });
    }

    @Test
    @DisplayName("RL-06 a non-positive limit fails startup")
    void rejectsNonPositiveLimit() {
        runner.withPropertyValues("rate-limit.max-requests=0", "rate-limit.window=1m", "rate-limit.paths=/a/**")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("RL-06 a window shorter than one second fails startup")
    void rejectsTooShortWindow() {
        runner.withPropertyValues("rate-limit.max-requests=10", "rate-limit.window=0s", "rate-limit.paths=/a/**")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("RL-06 a missing window fails startup")
    void rejectsMissingWindow() {
        runner.withPropertyValues("rate-limit.max-requests=10", "rate-limit.paths=/a/**")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(RateLimitProperties.class)
    static class PropertiesConfig {}
}
