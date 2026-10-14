package com.interviewprep.expense.mapper;

import com.interviewprep.expense.dto.ExpenseRequest;
import com.interviewprep.expense.dto.ExpenseResponse;
import com.interviewprep.expense.entity.Expense;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

    public Expense toEntity(ExpenseRequest request) {
        return new Expense(toAmountScale(request.amount()), request.category(), request.date(), request.note());
    }

    public void applyTo(Expense expense, ExpenseRequest request) {
        expense.revise(toAmountScale(request.amount()), request.category(), request.date(), request.note());
    }

    public ExpenseResponse toResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                toAmountScale(expense.getAmount()),
                expense.getCategory(),
                expense.getDate(),
                expense.getNote());
    }

    private static BigDecimal toAmountScale(BigDecimal amount) {
        return amount.setScale(Expense.AMOUNT_SCALE, RoundingMode.UNNECESSARY);
    }
}
