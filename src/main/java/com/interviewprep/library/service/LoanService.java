package com.interviewprep.library.service;

import com.interviewprep.library.dto.BorrowRequest;
import com.interviewprep.library.dto.LoanResponse;
import com.interviewprep.library.entity.Loan;
import com.interviewprep.library.exception.BookNotBorrowedException;
import com.interviewprep.library.exception.BookUnavailableException;
import com.interviewprep.library.mapper.LoanMapper;
import com.interviewprep.library.repository.BookRepository;
import com.interviewprep.library.repository.LoanRepository;
import com.interviewprep.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoanService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final Clock clock;

    public LoanService(
            BookRepository bookRepository, LoanRepository loanRepository, LoanMapper loanMapper, Clock clock) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.clock = clock;
    }

    public LoanResponse borrow(UUID bookId, BorrowRequest request) {
        boolean claimed = bookRepository.markBorrowedIfAvailable(bookId) == 1;
        if (!claimed) {
            throw bookRepository.existsById(bookId)
                    ? new BookUnavailableException(bookId)
                    : new NotFoundException("Book", bookId);
        }
        Loan loan = new Loan(
                bookRepository.getReferenceById(bookId), request.memberId().strip(), Instant.now(clock));
        return loanMapper.toResponse(loanRepository.save(loan));
    }

    public LoanResponse returnBook(UUID bookId) {
        boolean released = bookRepository.markReturnedIfBorrowed(bookId) == 1;
        if (!released) {
            throw bookRepository.existsById(bookId)
                    ? new BookNotBorrowedException(bookId)
                    : new NotFoundException("Book", bookId);
        }
        Loan loan = loanRepository
                .findByBookIdAndReturnedAtIsNull(bookId)
                .orElseThrow(() -> new BookNotBorrowedException(bookId));
        loan.markReturned(Instant.now(clock));
        return loanMapper.toResponse(loan);
    }
}
