package com.interviewprep.expense.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewprep.expense.dto.ExpenseRequest;
import com.interviewprep.expense.dto.ExpenseResponse;
import com.interviewprep.expense.entity.ExpenseCategory;
import com.interviewprep.expense.service.ExpenseService;
import com.interviewprep.shared.error.NotFoundException;
import com.interviewprep.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExpenseController.class)
class ExpenseControllerTest {

    private static final UUID EXPENSE_ID = UUID.fromString("7f1c2d3e-4a5b-4c6d-8e9f-0a1b2c3d4e5f");
    private static final String VALID_BODY = """
            {"amount": 12.50, "category": "FOOD", "date": "2026-10-01", "note": "Lunch"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService service;

    @Test
    @DisplayName("EXP-01 creating a valid expense returns 201 with a Location header and the stored expense")
    void createReturnsCreated() throws Exception {
        given(service.create(any())).willReturn(lunch());

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/expenses/" + EXPENSE_ID))
                .andExpect(jsonPath("$.id").value(EXPENSE_ID.toString()))
                .andExpect(jsonPath("$.amount").value(12.50))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.date").value("2026-10-01"))
                .andExpect(jsonPath("$.note").value("Lunch"));

        ArgumentCaptor<ExpenseRequest> captured = ArgumentCaptor.forClass(ExpenseRequest.class);
        verify(service).create(captured.capture());
        assertThat(captured.getValue().amount()).isEqualByComparingTo("12.50");
        assertThat(captured.getValue().category()).isEqualTo(ExpenseCategory.FOOD);
    }

    @Test
    @DisplayName("EXP-02 a negative amount is rejected with 400 and a field error")
    void negativeAmountRejected() throws Exception {
        String body = """
                {"amount": -5.00, "category": "FOOD", "date": "2026-10-01"}
                """;

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("amount"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("EXP-02 a zero amount is rejected with 400")
    void zeroAmountRejected() throws Exception {
        String body = """
                {"amount": 0, "category": "FOOD", "date": "2026-10-01"}
                """;

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("EXP-02 an amount with three decimal places is rejected with 400")
    void threeDecimalAmountRejected() throws Exception {
        String body = """
                {"amount": 1.005, "category": "FOOD", "date": "2026-10-01"}
                """;

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("EXP-02 a missing category is rejected with 400")
    void missingCategoryRejected() throws Exception {
        String body = """
                {"amount": 5.00, "date": "2026-10-01"}
                """;

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("category"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("EXP-02 a missing date and an oversized note are rejected with 400")
    void missingDateAndLongNoteRejected() throws Exception {
        String body = "{\"amount\": 5.00, \"category\": \"BILLS\", \"note\": \"" + "x".repeat(501) + "\"}";

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'date')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'note')]").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("EXP-02 an unknown category value is rejected with 400")
    void unknownCategoryRejected() throws Exception {
        String body = """
                {"amount": 5.00, "category": "LEISURE", "date": "2026-10-01"}
                """;

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("EXP-03 listing returns a page of expenses")
    void listReturnsPage() throws Exception {
        given(service.list(any())).willReturn(new PageResponse<>(List.of(lunch()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(EXPENSE_ID.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("EXP-04 fetching an existing expense returns 200 with the expense")
    void getReturnsExpense() throws Exception {
        given(service.get(EXPENSE_ID)).willReturn(lunch());

        mockMvc.perform(get("/api/v1/expenses/{id}", EXPENSE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EXPENSE_ID.toString()))
                .andExpect(jsonPath("$.category").value("FOOD"));
    }

    @Test
    @DisplayName("EXP-05 fetching an unknown expense returns 404 problem detail")
    void getUnknownReturnsNotFound() throws Exception {
        given(service.get(EXPENSE_ID)).willThrow(new NotFoundException("Expense", EXPENSE_ID));

        mockMvc.perform(get("/api/v1/expenses/{id}", EXPENSE_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    @DisplayName("EXP-05 a malformed expense id returns 400")
    void malformedIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/expenses/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("EXP-06 updating an existing expense returns 200 with the revised expense")
    void updateReturnsRevisedExpense() throws Exception {
        ExpenseResponse revised = new ExpenseResponse(
                EXPENSE_ID, new BigDecimal("40.00"), ExpenseCategory.TRAVEL, LocalDate.of(2026, 10, 2), null);
        given(service.update(eq(EXPENSE_ID), any())).willReturn(revised);
        String body = """
                {"amount": 40, "category": "TRAVEL", "date": "2026-10-02"}
                """;

        mockMvc.perform(put("/api/v1/expenses/{id}", EXPENSE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(40.00))
                .andExpect(jsonPath("$.category").value("TRAVEL"))
                .andExpect(jsonPath("$.date").value("2026-10-02"));
    }

    @Test
    @DisplayName("EXP-06 updating with an invalid body returns 400")
    void updateWithInvalidBodyRejected() throws Exception {
        String body = """
                {"amount": -1, "category": "TRAVEL", "date": "2026-10-02"}
                """;

        mockMvc.perform(put("/api/v1/expenses/{id}", EXPENSE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("EXP-05 updating an unknown expense returns 404")
    void updateUnknownReturnsNotFound() throws Exception {
        given(service.update(eq(EXPENSE_ID), any())).willThrow(new NotFoundException("Expense", EXPENSE_ID));

        mockMvc.perform(put("/api/v1/expenses/{id}", EXPENSE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("EXP-07 deleting an existing expense returns 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/expenses/{id}", EXPENSE_ID)).andExpect(status().isNoContent());

        verify(service).delete(EXPENSE_ID);
    }

    @Test
    @DisplayName("EXP-05 deleting an unknown expense returns 404")
    void deleteUnknownReturnsNotFound() throws Exception {
        willThrow(new NotFoundException("Expense", EXPENSE_ID)).given(service).delete(EXPENSE_ID);

        mockMvc.perform(delete("/api/v1/expenses/{id}", EXPENSE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private static ExpenseResponse lunch() {
        return new ExpenseResponse(
                EXPENSE_ID, new BigDecimal("12.50"), ExpenseCategory.FOOD, LocalDate.of(2026, 10, 1), "Lunch");
    }
}
