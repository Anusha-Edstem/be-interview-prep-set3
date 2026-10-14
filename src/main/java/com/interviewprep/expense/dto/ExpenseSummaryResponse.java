package com.interviewprep.expense.dto;

import com.interviewprep.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

public record ExpenseSummaryResponse(YearMonth month, Map<ExpenseCategory, BigDecimal> totals, BigDecimal overall) {}
