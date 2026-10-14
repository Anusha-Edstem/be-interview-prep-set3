package com.interviewprep.expense.dto;

import com.interviewprep.expense.entity.ExpenseCategory;
import java.time.LocalDate;

public record ExpenseFilter(LocalDate from, LocalDate to, ExpenseCategory category) {

    public boolean hasInvertedRange() {
        return from != null && to != null && from.isAfter(to);
    }
}
