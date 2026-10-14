package com.interviewprep.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.expense.repository.ExpenseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository repository;

    @BeforeEach
    @AfterEach
    void clearExpenses() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("EXP-10 the monthly summary counts the first and last day of the month and totals 0.10 + 0.20 as 0.30")
    void monthlySummaryIsExactAtMonthBoundaries() throws Exception {
        record("0.10", "FOOD", "2026-10-01");
        record("0.20", "FOOD", "2026-10-31");
        record("5.00", "TRAVEL", "2026-09-30");
        record("7.00", "BILLS", "2026-11-01");

        String body = mockMvc.perform(get("/api/v1/expenses/summary").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .isEqualTo("{\"month\":\"2026-10\",\"totals\":{\"FOOD\":0.30,\"TRAVEL\":0.00,"
                        + "\"BILLS\":0.00,\"OTHER\":0.00},\"overall\":0.30}");
        JsonNode summary = objectMapper.readTree(body);
        assertThat(summary.get("totals").get("FOOD").decimalValue()).isEqualByComparingTo("0.30");
        assertThat(summary.get("overall").decimalValue()).isEqualByComparingTo("0.30");
    }

    @Test
    @DisplayName("EXP-10 the neighbouring months only see their own boundary expenses")
    void neighbouringMonthsSeeTheirOwnBoundaryExpenses() throws Exception {
        record("0.10", "FOOD", "2026-10-01");
        record("5.00", "TRAVEL", "2026-09-30");
        record("7.00", "BILLS", "2026-11-01");

        mockMvc.perform(get("/api/v1/expenses/summary").param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"TRAVEL\":5.00")))
                .andExpect(content().string(containsString("\"overall\":5.00")));
        mockMvc.perform(get("/api/v1/expenses/summary").param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"BILLS\":7.00")))
                .andExpect(content().string(containsString("\"overall\":7.00")));
    }

    @Test
    @DisplayName("EXP-03 a requested page size above 100 is capped at 100")
    void pageSizeIsCapped() throws Exception {
        record("1.00", "OTHER", "2026-10-05");

        mockMvc.perform(get("/api/v1/expenses").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(1.00));
    }

    private void record(String amount, String category, String date) throws Exception {
        String body = "{\"amount\":" + amount + ",\"category\":\"" + category + "\",\"date\":\"" + date + "\"}";
        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
