package com.interviewprep.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewprep.library.dto.BookRequest;
import com.interviewprep.library.dto.BorrowRequest;
import com.interviewprep.library.entity.Book;
import com.interviewprep.library.exception.BookUnavailableException;
import com.interviewprep.library.repository.BookRepository;
import com.interviewprep.library.repository.LoanRepository;
import com.interviewprep.library.service.BookService;
import com.interviewprep.library.service.LoanService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class ConcurrentBorrowIntegrationTest {

    private static final int BORROWERS = 10;
    private static final long TIMEOUT_SECONDS = 30;

    @Autowired
    private BookService bookService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final List<UUID> createdBookIds = new ArrayList<>();

    @AfterEach
    void removeCreatedBooks() {
        bookRepository.deleteAllById(createdBookIds);
    }

    @Test
    @DisplayName("LIB-13 ten members borrowing the same book at once leaves exactly one loan")
    void onlyOneConcurrentBorrowSucceeds() throws Exception {
        UUID bookId = createBook("9785555555551");
        CountDownLatch ready = new CountDownLatch(BORROWERS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService borrowers = Executors.newFixedThreadPool(BORROWERS);
        List<Boolean> outcomes = new ArrayList<>();

        try {
            List<Future<Boolean>> attempts = new ArrayList<>();
            for (int member = 0; member < BORROWERS; member++) {
                String memberId = "member-" + member;
                attempts.add(borrowers.submit(() -> {
                    ready.countDown();
                    start.await();
                    return tryBorrow(bookId, memberId);
                }));
            }
            assertThat(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Boolean> attempt : attempts) {
                outcomes.add(attempt.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
        } finally {
            borrowers.shutdownNow();
        }

        assertThat(outcomes).hasSize(BORROWERS).containsOnlyOnce(true);
        assertThat(loansOf(bookId)).isEqualTo(1);
        assertThat(bookService.get(bookId).available()).isFalse();
    }

    @Test
    @DisplayName("LIB-07 updating a book loaded before a concurrent borrow does not make it available again")
    void staleUpdateKeepsBookBorrowed() {
        UUID bookId = createBook("9785555555552");
        ExecutorService otherRequest = Executors.newSingleThreadExecutor();

        try {
            transactionTemplate.executeWithoutResult(status -> {
                Book loadedBeforeBorrow = bookRepository.findById(bookId).orElseThrow();
                CompletableFuture.runAsync(
                                () -> loanService.borrow(bookId, new BorrowRequest("member-1")), otherRequest)
                        .join();
                loadedBeforeBorrow.replaceDetails("Renamed", "Writer", "9785555555552", 2020);
            });
        } finally {
            otherRequest.shutdownNow();
        }

        assertThat(bookService.get(bookId).title()).isEqualTo("Renamed");
        assertThat(bookService.get(bookId).available()).isFalse();
        assertThatThrownBy(() -> loanService.borrow(bookId, new BorrowRequest("member-2")))
                .isInstanceOf(BookUnavailableException.class);
    }

    private UUID createBook(String isbn) {
        UUID id = bookService
                .create(new BookRequest("Contended", "Writer", isbn, 2020))
                .id();
        createdBookIds.add(id);
        return id;
    }

    private boolean tryBorrow(UUID bookId, String memberId) {
        try {
            loanService.borrow(bookId, new BorrowRequest(memberId));
            return true;
        } catch (BookUnavailableException unavailable) {
            return false;
        }
    }

    private long loansOf(UUID bookId) {
        return loanRepository.findAll().stream()
                .filter(loan -> loan.getBook().getId().equals(bookId))
                .count();
    }
}
