package com.interviewprep.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
class RateLimitIntegrationTest {

    private static final String QUOTE_PATH = "/api/v1/quotes/random";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SlidingWindowRateLimiter limiter;

    @Autowired
    private MutableClock clock;

    @Test
    @DisplayName("RL-02 ten requests succeed and the eleventh in the same minute is rejected with 429")
    void eleventhRequestRejected() throws Exception {
        String apiKey = uniqueKey();

        for (int i = 1; i <= 10; i++) {
            requestQuote(apiKey)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").isNotEmpty())
                    .andExpect(header().string(RateLimitInterceptor.LIMIT_HEADER, "10"))
                    .andExpect(header().string(RateLimitInterceptor.REMAINING_HEADER, String.valueOf(10 - i)));
        }
        ResultActions eleventh = requestQuote(apiKey);

        eleventh.andExpect(status().isTooManyRequests())
                .andExpect(header().string(RateLimitInterceptor.LIMIT_HEADER, "10"))
                .andExpect(header().string(RateLimitInterceptor.REMAINING_HEADER, "0"))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/rate-limit-exceeded"))
                .andExpect(jsonPath("$.title").value("Too many requests"))
                .andExpect(jsonPath("$.detail").value("Rate limit exceeded, retry after 60 seconds"))
                .andExpect(jsonPath("$.instance").value(QUOTE_PATH))
                .andExpect(jsonPath("$.retryAfterSeconds").value(60));
    }

    @Test
    @DisplayName("RL-02 retry-after counts down and the client is served again once the window has passed")
    void servedAgainAfterWindow() throws Exception {
        String apiKey = uniqueKey();
        for (int i = 0; i < 10; i++) {
            requestQuote(apiKey);
        }

        clock.advance(Duration.ofSeconds(45));
        ResultActions stillLimited = requestQuote(apiKey);
        clock.advance(Duration.ofSeconds(15));
        ResultActions afterWindow = requestQuote(apiKey);

        stillLimited
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "15"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(15));
        afterWindow.andExpect(status().isOk()).andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("RL-03 a second API key is still served after the first is rate limited")
    void otherKeyUnaffected() throws Exception {
        String limitedKey = uniqueKey();
        String otherKey = uniqueKey();
        for (int i = 0; i < 11; i++) {
            requestQuote(limitedKey);
        }

        ResultActions other = requestQuote(otherKey);

        other.andExpect(status().isOk()).andExpect(header().string(RateLimitInterceptor.REMAINING_HEADER, "9"));
        requestQuote(limitedKey).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("RL-04 a request without an API key is rejected with 401")
    void missingApiKeyRejected() throws Exception {
        ResultActions result = mockMvc.perform(get(QUOTE_PATH));

        result.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/missing-api-key"));
    }

    @Test
    @DisplayName("RL-04 a request with a blank API key is rejected with 401")
    void blankApiKeyRejected() throws Exception {
        ResultActions result = requestQuote("   ");

        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/missing-api-key"));
    }

    @Test
    @DisplayName("RL-05 fifty simultaneous requests from one key admit exactly ten")
    void concurrentRequestsAdmitExactlyTheLimit() throws Exception {
        String apiKey = uniqueKey();
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<RateLimitDecision>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            futures.add(executor.submit(() -> {
                startGate.await();
                return limiter.tryAcquire(apiKey);
            }));
        }

        startGate.countDown();
        long allowed = 0;
        for (Future<RateLimitDecision> future : futures) {
            allowed += future.get(10, TimeUnit.SECONDS).allowed() ? 1 : 0;
        }
        executor.shutdown();

        assertThat(allowed).isEqualTo(10);
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        requestQuote(apiKey).andExpect(status().isTooManyRequests());
    }

    private ResultActions requestQuote(String apiKey) throws Exception {
        return mockMvc.perform(get(QUOTE_PATH).header(RateLimitInterceptor.API_KEY_HEADER, apiKey));
    }

    private static String uniqueKey() {
        return "client-" + UUID.randomUUID();
    }
}
