package com.interviewprep.expense.repository;

import com.interviewprep.expense.entity.Expense;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {}
