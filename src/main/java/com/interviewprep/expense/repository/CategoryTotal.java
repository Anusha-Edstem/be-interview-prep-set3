package com.interviewprep.expense.repository;

import com.interviewprep.expense.entity.ExpenseCategory;
import java.math.BigDecimal;

public record CategoryTotal(ExpenseCategory category, BigDecimal total) {}
