package com.interviewprep.expense.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewprep.expense.entity.Expense;
import com.interviewprep.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExpenseRepositoryTest {

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
                new Expense(new BigDecimal("12.34"), ExpenseCategory.TRAVEL, LocalDate.of(2026, 10, 1), "Taxi"));
        entityManager.clear();

        Expense reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getAmount()).isEqualByComparingTo("12.34");
        assertThat(reloaded.getAmount().scale()).isEqualTo(2);
        assertThat(reloaded.getCategory()).isEqualTo(ExpenseCategory.TRAVEL);
        assertThat(reloaded.getDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(reloaded.getNote()).isEqualTo("Taxi");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }
}
