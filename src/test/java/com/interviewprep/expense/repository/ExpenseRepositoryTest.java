package com.interviewprep.expense.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.interviewprep.expense.entity.Expense;
import com.interviewprep.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExpenseRepositoryTest {

    private static final LocalDate LAST_DAY_OF_SEPTEMBER = LocalDate.of(2026, 9, 30);
    private static final LocalDate FIRST_DAY_OF_OCTOBER = LocalDate.of(2026, 10, 1);
    private static final LocalDate MID_OCTOBER = LocalDate.of(2026, 10, 15);
    private static final LocalDate LAST_DAY_OF_OCTOBER = LocalDate.of(2026, 10, 31);
    private static final LocalDate FIRST_DAY_OF_NOVEMBER = LocalDate.of(2026, 11, 1);
    private static final PageRequest NEWEST_FIRST = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "date"));

    @Autowired
    private ExpenseRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void clearExpenses() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("EXP-01 an expense persists with an exact two-decimal amount and a generated id")
    void persistsExpenseWithExactAmount() {
        Expense saved = repository.saveAndFlush(
                new Expense(new BigDecimal("12.34"), ExpenseCategory.TRAVEL, FIRST_DAY_OF_OCTOBER, "Taxi"));
        entityManager.clear();

        Expense reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getAmount()).isEqualByComparingTo("12.34");
        assertThat(reloaded.getAmount().scale()).isEqualTo(2);
        assertThat(reloaded.getCategory()).isEqualTo(ExpenseCategory.TRAVEL);
        assertThat(reloaded.getDate()).isEqualTo(FIRST_DAY_OF_OCTOBER);
        assertThat(reloaded.getNote()).isEqualTo("Taxi");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("EXP-08 a date range includes both of its bounds and excludes the days outside it")
    void dateRangeIsInclusive() {
        storeAroundOctober();

        Page<Expense> october = repository.findAll(
                ExpenseSpecifications.matching(FIRST_DAY_OF_OCTOBER, LAST_DAY_OF_OCTOBER, null), NEWEST_FIRST);

        assertThat(october.getContent())
                .extracting(Expense::getDate)
                .containsExactly(LAST_DAY_OF_OCTOBER, MID_OCTOBER, FIRST_DAY_OF_OCTOBER);
    }

    @Test
    @DisplayName("EXP-08 a date range combined with a category keeps only matching expenses")
    void dateRangeAndCategoryCombine() {
        storeAroundOctober();

        Page<Expense> octoberFood = repository.findAll(
                ExpenseSpecifications.matching(FIRST_DAY_OF_OCTOBER, LAST_DAY_OF_OCTOBER, ExpenseCategory.FOOD),
                NEWEST_FIRST);

        assertThat(octoberFood.getContent())
                .extracting(Expense::getDate, Expense::getCategory)
                .containsExactly(
                        tuple(LAST_DAY_OF_OCTOBER, ExpenseCategory.FOOD),
                        tuple(FIRST_DAY_OF_OCTOBER, ExpenseCategory.FOOD));
    }

    @Test
    @DisplayName("EXP-08 either bound of the range is optional")
    void eitherBoundIsOptional() {
        storeAroundOctober();

        List<LocalDate> fromOctober = datesOf(
                repository.findAll(ExpenseSpecifications.matching(FIRST_DAY_OF_OCTOBER, null, null), NEWEST_FIRST));
        List<LocalDate> untilOctober = datesOf(
                repository.findAll(ExpenseSpecifications.matching(null, LAST_DAY_OF_OCTOBER, null), NEWEST_FIRST));
        List<LocalDate> everything =
                datesOf(repository.findAll(ExpenseSpecifications.matching(null, null, null), NEWEST_FIRST));

        assertThat(fromOctober)
                .containsExactly(FIRST_DAY_OF_NOVEMBER, LAST_DAY_OF_OCTOBER, MID_OCTOBER, FIRST_DAY_OF_OCTOBER);
        assertThat(untilOctober)
                .containsExactly(LAST_DAY_OF_OCTOBER, MID_OCTOBER, FIRST_DAY_OF_OCTOBER, LAST_DAY_OF_SEPTEMBER);
        assertThat(everything).hasSize(5);
    }

    @Test
    @DisplayName("EXP-10 monthly sums include the first and last day of the month and exclude the neighbouring days")
    void monthlySumsHonourMonthBoundaries() {
        storeAroundOctober();

        List<CategoryTotal> totals = repository.sumByCategoryBetween(FIRST_DAY_OF_OCTOBER, LAST_DAY_OF_OCTOBER);

        assertThat(totals)
                .extracting(CategoryTotal::category)
                .containsExactlyInAnyOrder(ExpenseCategory.FOOD, ExpenseCategory.TRAVEL);
        assertThat(totalOf(totals, ExpenseCategory.FOOD)).isEqualByComparingTo("6.00");
        assertThat(totalOf(totals, ExpenseCategory.TRAVEL)).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("EXP-10 monthly sums are exact so 0.10 plus 0.20 is 0.30")
    void monthlySumsAreExact() {
        repository.saveAll(List.of(
                expense("0.10", ExpenseCategory.FOOD, FIRST_DAY_OF_OCTOBER),
                expense("0.20", ExpenseCategory.FOOD, LAST_DAY_OF_OCTOBER)));
        repository.flush();

        List<CategoryTotal> totals = repository.sumByCategoryBetween(FIRST_DAY_OF_OCTOBER, LAST_DAY_OF_OCTOBER);

        assertThat(totals).hasSize(1);
        assertThat(totalOf(totals, ExpenseCategory.FOOD)).isEqualByComparingTo("0.30");
    }

    private static BigDecimal totalOf(List<CategoryTotal> totals, ExpenseCategory category) {
        return totals.stream()
                .filter(total -> total.category() == category)
                .map(CategoryTotal::total)
                .findFirst()
                .orElseThrow();
    }

    private void storeAroundOctober() {
        repository.saveAll(List.of(
                expense("1.00", ExpenseCategory.FOOD, LAST_DAY_OF_SEPTEMBER),
                expense("2.00", ExpenseCategory.FOOD, FIRST_DAY_OF_OCTOBER),
                expense("3.00", ExpenseCategory.TRAVEL, MID_OCTOBER),
                expense("4.00", ExpenseCategory.FOOD, LAST_DAY_OF_OCTOBER),
                expense("5.00", ExpenseCategory.FOOD, FIRST_DAY_OF_NOVEMBER)));
        repository.flush();
    }

    private static Expense expense(String amount, ExpenseCategory category, LocalDate date) {
        return new Expense(new BigDecimal(amount), category, date, null);
    }

    private static List<LocalDate> datesOf(Page<Expense> page) {
        return page.getContent().stream().map(Expense::getDate).toList();
    }
}
