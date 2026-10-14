package com.interviewprep.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @Column(name = "member_id", nullable = false, updatable = false, length = 100)
    private String memberId;

    @Column(name = "borrowed_at", nullable = false, updatable = false)
    private Instant borrowedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    protected Loan() {}

    public Loan(Book book, String memberId, Instant borrowedAt) {
        this.book = book;
        this.memberId = memberId;
        this.borrowedAt = borrowedAt;
    }

    public void markReturned(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }

    public UUID getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public String getMemberId() {
        return memberId;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }
}
