package com.interviewprep.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.interviewprep.expense.dto.ExpenseFilter;
import com.interviewprep.expense.dto.ExpenseRequest;
import com.interviewprep.expense.dto.ExpenseResponse;
import com.interviewprep.expense.dto.ExpenseSummaryResponse;
import com.interviewprep.expense.entity.Expense;
import com.interviewprep.expense.entity.ExpenseCategory;
import com.interviewprep.expense.mapper.ExpenseMapper;
import com.interviewprep.expense.repository.CategoryTotal;
import com.interviewprep.expense.repository.ExpenseRepository;
import com.interviewprep.shared.error.NotFoundException;
import com.interviewprep.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final UUID EXPENSE_ID = UUID.fromString("7f1c2d3e-4a5b-4c6d-8e9f-0a1b2c3d4e5f");
    private static final ExpenseFilter NO_FILTER = new ExpenseFilter(null, null, null);

    @Mock
    private ExpenseRepository repository;

    private ExpenseService service;

    @BeforeEach
    void createService() {
        service = new ExpenseService(repository, new ExpenseMapper());
    }

    @Test
    @DisplayName("EXP-01 create stores the expense with a two-decimal amount and returns it")
    void createStoresExpense() {
        ExpenseRequest request =
                new ExpenseRequest(new BigDecimal("7.5"), ExpenseCategory.FOOD, LocalDate.of(2026, 10, 1), "Coffee");
        given(repository.save(any(Expense.class))).willAnswer(invocation -> withId(invocation.getArgument(0)));

        ExpenseResponse response = service.create(request);

        ArgumentCaptor<Expense> saved = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("7.50");
        assertThat(saved.getValue().getAmount().scale()).isEqualTo(2);
        assertThat(response.id()).isEqualTo(EXPENSE_ID);
        assertThat(response.amount()).hasToString("7.50");
        assertThat(response.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(response.note()).isEqualTo("Coffee");
    }

    @Test
    @DisplayName("EXP-03 list sorts newest first and caps the page size at 100")
    void listCapsPageSizeAndSortsNewestFirst() {
        Expense stored =
                withId(new Expense(new BigDecimal("3.00"), ExpenseCategory.OTHER, LocalDate.of(2026, 10, 3), null));
        given(repository.findAll(anySpecification(), any(Pageable.class)))
                .willAnswer(invocation -> new PageImpl<>(List.of(stored), invocation.getArgument(1), 1));

        PageResponse<ExpenseResponse> page = service.list(NO_FILTER, PageRequest.of(2, 500));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(anySpecification(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(ExpenseService.MAX_PAGE_SIZE);
        assertThat(pageable.getValue().getSort().getOrderFor("date")).isEqualTo(Sort.Order.desc("date"));
        assertThat(page.size()).isEqualTo(100);
        assertThat(page.content()).extracting(ExpenseResponse::id).containsExactly(EXPENSE_ID);
    }

    @Test
    @DisplayName("EXP-08 list accepts a single-day range where from equals to")
    void listAcceptsSingleDayRange() {
        LocalDate day = LocalDate.of(2026, 10, 31);
        given(repository.findAll(anySpecification(), any(Pageable.class)))
                .willAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(1), 0));

        PageResponse<ExpenseResponse> page =
                service.list(new ExpenseFilter(day, day, ExpenseCategory.BILLS), PageRequest.of(0, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    @DisplayName("EXP-09 list rejects a range whose from is after its to without querying")
    void listRejectsInvertedRange() {
        ExpenseFilter inverted = new ExpenseFilter(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1), null);

        assertThatThrownBy(() -> service.list(inverted, PageRequest.of(0, 20)))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("2026-10-31")
                .hasMessageContaining("2026-10-01");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("EXP-04 get returns the stored expense")
    void getReturnsExpense() {
        given(repository.findById(EXPENSE_ID)).willReturn(Optional.of(lunch()));

        ExpenseResponse response = service.get(EXPENSE_ID);

        assertThat(response.id()).isEqualTo(EXPENSE_ID);
        assertThat(response.amount()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("EXP-05 get of an unknown expense raises not found")
    void getUnknownRaisesNotFound() {
        given(repository.findById(EXPENSE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(EXPENSE_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(EXPENSE_ID.toString());
    }

    @Test
    @DisplayName("EXP-06 update revises every field of the stored expense")
    void updateRevisesExpense() {
        Expense stored = lunch();
        given(repository.findById(EXPENSE_ID)).willReturn(Optional.of(stored));
        ExpenseRequest request =
                new ExpenseRequest(new BigDecimal("99.99"), ExpenseCategory.BILLS, LocalDate.of(2026, 10, 31), null);

        ExpenseResponse response = service.update(EXPENSE_ID, request);

        assertThat(stored.getAmount()).isEqualByComparingTo("99.99");
        assertThat(stored.getCategory()).isEqualTo(ExpenseCategory.BILLS);
        assertThat(stored.getDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(stored.getNote()).isNull();
        assertThat(response.category()).isEqualTo(ExpenseCategory.BILLS);
    }

    @Test
    @DisplayName("EXP-05 update of an unknown expense raises not found")
    void updateUnknownRaisesNotFound() {
        given(repository.findById(EXPENSE_ID)).willReturn(Optional.empty());
        ExpenseRequest request =
                new ExpenseRequest(BigDecimal.ONE, ExpenseCategory.BILLS, LocalDate.of(2026, 10, 31), null);

        assertThatThrownBy(() -> service.update(EXPENSE_ID, request)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("EXP-07 delete removes the stored expense")
    void deleteRemovesExpense() {
        Expense stored = lunch();
        given(repository.findById(EXPENSE_ID)).willReturn(Optional.of(stored));

        service.delete(EXPENSE_ID);

        verify(repository).delete(stored);
    }

    @Test
    @DisplayName("EXP-05 delete of an unknown expense raises not found and deletes nothing")
    void deleteUnknownRaisesNotFound() {
        given(repository.findById(EXPENSE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(EXPENSE_ID)).isInstanceOf(NotFoundException.class);

        verify(repository, never()).delete(any(Expense.class));
    }

    private static Expense lunch() {
        return withId(new Expense(new BigDecimal("12.50"), ExpenseCategory.FOOD, LocalDate.of(2026, 10, 1), "Lunch"));
    }

    @Test
    @DisplayName("EXP-10 summary queries from the first to the last day of the month inclusive")
    void summaryQueriesWholeMonth() {
        given(repository.sumByCategoryBetween(any(), any())).willReturn(List.of());

        service.summarize(YearMonth.of(2028, 2));

        verify(repository).sumByCategoryBetween(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("EXP-10 summary lists every category at scale 2 and adds the overall total exactly")
    void summaryFillsEveryCategoryAndAddsExactly() {
        given(repository.sumByCategoryBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .willReturn(List.of(
                        new CategoryTotal(ExpenseCategory.FOOD, new BigDecimal("0.30")),
                        new CategoryTotal(ExpenseCategory.BILLS, new BigDecimal("1000000.1"))));

        ExpenseSummaryResponse summary = service.summarize(YearMonth.of(2026, 10));

        assertThat(summary.month()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(summary.totals()).containsOnlyKeys(ExpenseCategory.values());
        assertThat(summary.totals().get(ExpenseCategory.FOOD)).hasToString("0.30");
        assertThat(summary.totals().get(ExpenseCategory.BILLS)).hasToString("1000000.10");
        assertThat(summary.totals().get(ExpenseCategory.TRAVEL)).hasToString("0.00");
        assertThat(summary.totals().get(ExpenseCategory.OTHER)).hasToString("0.00");
        assertThat(summary.overall()).hasToString("1000000.40");
    }

    @Test
    @DisplayName("EXP-10 summary of a month without expenses is all zeros")
    void summaryOfEmptyMonthIsZero() {
        given(repository.sumByCategoryBetween(any(), any())).willReturn(List.of());

        ExpenseSummaryResponse summary = service.summarize(YearMonth.of(2026, 12));

        assertThat(summary.totals().values())
                .allSatisfy(total -> assertThat(total).hasToString("0.00"));
        assertThat(summary.overall()).hasToString("0.00");
    }

    private static Specification<Expense> anySpecification() {
        return any();
    }

    private static Expense withId(Expense expense) {
        ReflectionTestUtils.setField(expense, "id", EXPENSE_ID);
        return expense;
    }
}
