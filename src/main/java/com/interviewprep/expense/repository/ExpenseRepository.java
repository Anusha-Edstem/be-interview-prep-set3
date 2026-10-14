package com.interviewprep.expense.repository;

import com.interviewprep.expense.entity.Expense;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    @Query("select new com.interviewprep.expense.repository.CategoryTotal(e.category, sum(e.amount))"
            + " from Expense e where e.date between :start and :end group by e.category")
    List<CategoryTotal> sumByCategoryBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
