package com.interviewprep.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.interviewprep.library.dto.BorrowRequest;
import com.interviewprep.library.dto.LoanResponse;
import com.interviewprep.library.entity.Book;
import com.interviewprep.library.entity.Loan;
import com.interviewprep.library.exception.BookNotBorrowedException;
import com.interviewprep.library.exception.BookUnavailableException;
import com.interviewprep.library.mapper.LoanMapper;
import com.interviewprep.library.repository.BookRepository;
import com.interviewprep.library.repository.LoanRepository;
import com.interviewprep.shared.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final UUID BOOK_ID = UUID.fromString("7d1b6f3e-2c4a-4f0e-9a51-3f1f2b9c8e10");
    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService =
                new LoanService(bookRepository, loanRepository, new LoanMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("LIB-11 borrow claims the book and records a loan for the member at the current time")
    void borrowRecordsLoan() {
        Book book = new Book("Clean Code", "Robert C. Martin", "9780132350884", 2008);
        given(bookRepository.markBorrowedIfAvailable(BOOK_ID)).willReturn(1);
        given(bookRepository.getReferenceById(BOOK_ID)).willReturn(book);
        given(loanRepository.save(any(Loan.class))).willAnswer(invocation -> invocation.getArgument(0));

        LoanResponse response = loanService.borrow(BOOK_ID, new BorrowRequest(" member-42 "));

        ArgumentCaptor<Loan> saved = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(saved.capture());
        assertThat(saved.getValue().getBook()).isSameAs(book);
        assertThat(response.memberId()).isEqualTo("member-42");
        assertThat(response.borrowedAt()).isEqualTo(NOW);
        assertThat(response.returnedAt()).isNull();
    }

    @Test
    @DisplayName("LIB-12 borrow rejects a book that is already borrowed and records no loan")
    void borrowRejectsUnavailableBook() {
        given(bookRepository.markBorrowedIfAvailable(BOOK_ID)).willReturn(0);
        given(bookRepository.existsById(BOOK_ID)).willReturn(true);

        assertThatThrownBy(() -> loanService.borrow(BOOK_ID, new BorrowRequest("member-42")))
                .isInstanceOf(BookUnavailableException.class)
                .hasMessageContaining(BOOK_ID.toString());
        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("LIB-11 borrow raises not found for an unknown book")
    void borrowUnknownRaisesNotFound() {
        given(bookRepository.markBorrowedIfAvailable(BOOK_ID)).willReturn(0);
        given(bookRepository.existsById(BOOK_ID)).willReturn(false);

        assertThatThrownBy(() -> loanService.borrow(BOOK_ID, new BorrowRequest("member-42")))
                .isInstanceOf(NotFoundException.class);
        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("LIB-14 return releases the book and closes the open loan at the current time")
    void returnClosesOpenLoan() {
        Book book = new Book("Clean Code", "Robert C. Martin", "9780132350884", 2008);
        Loan openLoan = new Loan(book, "member-42", NOW.minusSeconds(86_400));
        given(bookRepository.markReturnedIfBorrowed(BOOK_ID)).willReturn(1);
        given(loanRepository.findByBookIdAndReturnedAtIsNull(BOOK_ID)).willReturn(Optional.of(openLoan));

        LoanResponse response = loanService.returnBook(BOOK_ID);

        assertThat(openLoan.getReturnedAt()).isEqualTo(NOW);
        assertThat(response.returnedAt()).isEqualTo(NOW);
        assertThat(response.memberId()).isEqualTo("member-42");
    }

    @Test
    @DisplayName("LIB-15 return rejects a book that is not borrowed")
    void returnRejectsBookNotBorrowed() {
        given(bookRepository.markReturnedIfBorrowed(BOOK_ID)).willReturn(0);
        given(bookRepository.existsById(BOOK_ID)).willReturn(true);

        assertThatThrownBy(() -> loanService.returnBook(BOOK_ID)).isInstanceOf(BookNotBorrowedException.class);
        verify(loanRepository, never()).findByBookIdAndReturnedAtIsNull(any());
    }

    @Test
    @DisplayName("LIB-14 return raises not found for an unknown book")
    void returnUnknownRaisesNotFound() {
        given(bookRepository.markReturnedIfBorrowed(BOOK_ID)).willReturn(0);
        given(bookRepository.existsById(BOOK_ID)).willReturn(false);

        assertThatThrownBy(() -> loanService.returnBook(BOOK_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("LIB-15 return rejects a borrowed flag with no open loan so the transaction rolls back")
    void returnWithoutOpenLoanRaisesNotBorrowed() {
        given(bookRepository.markReturnedIfBorrowed(BOOK_ID)).willReturn(1);
        given(loanRepository.findByBookIdAndReturnedAtIsNull(BOOK_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnBook(BOOK_ID)).isInstanceOf(BookNotBorrowedException.class);
    }
}
