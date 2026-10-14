package com.interviewprep.expense.repository;

import com.interviewprep.expense.entity.Expense;
import com.interviewprep.expense.entity.ExpenseCategory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ExpenseSpecifications {

    private ExpenseSpecifications() {}

    public static Specification<Expense> matching(LocalDate from, LocalDate to, ExpenseCategory category) {
        List<Specification<Expense>> clauses = new ArrayList<>();
        if (from != null) {
            clauses.add(onOrAfter(from));
        }
        if (to != null) {
            clauses.add(onOrBefore(to));
        }
        if (category != null) {
            clauses.add(inCategory(category));
        }
        return Specification.allOf(clauses);
    }

    private static Specification<Expense> onOrAfter(LocalDate from) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("date"), from);
    }

    private static Specification<Expense> onOrBefore(LocalDate to) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("date"), to);
    }

    private static Specification<Expense> inCategory(ExpenseCategory category) {
        return (root, query, builder) -> builder.equal(root.get("category"), category);
    }
}
