package com.interviewprep.expense.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    public static final int AMOUNT_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "amount", nullable = false, precision = 12, scale = AMOUNT_SCALE)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 16)
    private ExpenseCategory category;

    @Column(name = "expense_date", nullable = false)
    private LocalDate date;

    @Column(name = "note", length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Expense(BigDecimal amount, ExpenseCategory category, LocalDate date, String note) {
        revise(amount, category, date, note);
    }

    public void revise(BigDecimal amount, ExpenseCategory category, LocalDate date, String note) {
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }
}
