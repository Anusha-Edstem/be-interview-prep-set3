package com.interviewprep.expense.dto;

import com.interviewprep.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(UUID id, BigDecimal amount, ExpenseCategory category, LocalDate date, String note) {}
