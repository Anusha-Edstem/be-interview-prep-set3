package com.interviewprep.expense.service;

import com.interviewprep.expense.dto.ExpenseFilter;
import com.interviewprep.expense.dto.ExpenseRequest;
import com.interviewprep.expense.dto.ExpenseResponse;
import com.interviewprep.expense.dto.ExpenseSummaryResponse;
import com.interviewprep.expense.entity.Expense;
import com.interviewprep.expense.entity.ExpenseCategory;
import com.interviewprep.expense.mapper.ExpenseMapper;
import com.interviewprep.expense.repository.CategoryTotal;
import com.interviewprep.expense.repository.ExpenseRepository;
import com.interviewprep.expense.repository.ExpenseSpecifications;
import com.interviewprep.shared.error.NotFoundException;
import com.interviewprep.shared.web.PageResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExpenseService {

    public static final int MAX_PAGE_SIZE = 100;

    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(Expense.AMOUNT_SCALE);

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("date"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ExpenseRepository repository;
    private final ExpenseMapper mapper;

    public ExpenseService(ExpenseRepository repository, ExpenseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public ExpenseResponse create(ExpenseRequest request) {
        Expense saved = repository.save(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(ExpenseFilter filter, Pageable requested) {
        if (filter.hasInvertedRange()) {
            throw new InvalidDateRangeException(filter.from(), filter.to());
        }
        Specification<Expense> matching = ExpenseSpecifications.matching(filter.from(), filter.to(), filter.category());
        return PageResponse.of(repository.findAll(matching, newestFirstCapped(requested)), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID id) {
        return mapper.toResponse(findExisting(id));
    }

    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = findExisting(id);
        mapper.applyTo(expense, request);
        return mapper.toResponse(expense);
    }

    public void delete(UUID id) {
        repository.delete(findExisting(id));
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse summarize(YearMonth month) {
        Map<ExpenseCategory, BigDecimal> totals = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : ExpenseCategory.values()) {
            totals.put(category, ZERO_AMOUNT);
        }
        for (CategoryTotal categoryTotal : repository.sumByCategoryBetween(month.atDay(1), month.atEndOfMonth())) {
            totals.put(categoryTotal.category(), toAmountScale(categoryTotal.total()));
        }
        BigDecimal overall = totals.values().stream().reduce(ZERO_AMOUNT, BigDecimal::add);
        return new ExpenseSummaryResponse(month, Collections.unmodifiableMap(totals), overall);
    }

    private Expense findExisting(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Expense", id));
    }

    private static BigDecimal toAmountScale(BigDecimal amount) {
        return amount.setScale(Expense.AMOUNT_SCALE, RoundingMode.UNNECESSARY);
    }

    private static Pageable newestFirstCapped(Pageable requested) {
        return PageRequest.of(
                requested.getPageNumber(), Math.min(requested.getPageSize(), MAX_PAGE_SIZE), NEWEST_FIRST);
    }
}
