package com.interviewprep.expense.dto;

import com.interviewprep.expense.entity.ExpenseCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull @Positive @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @NotNull ExpenseCategory category,
        @NotNull LocalDate date,
        @Size(max = 500) String note) {}
