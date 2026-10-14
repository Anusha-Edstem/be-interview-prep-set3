package com.interviewprep.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {"rate-limit.max-requests=3", "rate-limit.window=30s"})
@AutoConfigureMockMvc
@Import(MutableClockTestConfig.class)
class RateLimitConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Test
    @DisplayName("RL-06 a configured limit of three rejects the fourth request")
    void configuredLimitApplies() throws Exception {
        for (int i = 0; i < 3; i++) {
            requestQuote("configured-limit").andExpect(status().isOk());
        }

        ResultActions fourth = requestQuote("configured-limit");

        fourth.andExpect(status().isTooManyRequests())
                .andExpect(header().string(RateLimitInterceptor.LIMIT_HEADER, "3"))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(30));
    }

    @Test
    @DisplayName("RL-06 a configured window of thirty seconds frees the key after thirty seconds")
    void configuredWindowApplies() throws Exception {
        for (int i = 0; i < 3; i++) {
            requestQuote("configured-window");
        }

        clock.advance(Duration.ofSeconds(30));
        ResultActions afterWindow = requestQuote("configured-window");

        afterWindow.andExpect(status().isOk()).andExpect(header().string(RateLimitInterceptor.REMAINING_HEADER, "2"));
    }

    private ResultActions requestQuote(String apiKey) throws Exception {
        return mockMvc.perform(get("/api/v1/quotes/random").header(RateLimitInterceptor.API_KEY_HEADER, apiKey));
    }
}
