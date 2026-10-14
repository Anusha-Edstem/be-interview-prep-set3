package com.interviewprep.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewprep.library.entity.Book;
import com.interviewprep.library.entity.Loan;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Test
    @DisplayName("LIB-01 a saved book gets a generated id, timestamps and starts available")
    void savedBookGetsIdAndTimestamps() {
        Book book = new Book("Clean Code", "Robert C. Martin", "9780132350884", 2008);

        Book saved = bookRepository.saveAndFlush(book);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.isBorrowed()).isFalse();
        assertThat(bookRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("LIB-03 the database rejects a second book with the same ISBN")
    void uniqueIsbnIsEnforced() {
        bookRepository.saveAndFlush(new Book("Refactoring", "Martin Fowler", "9780134757599", 2018));
        Book duplicate = new Book("Refactoring copy", "Someone Else", "9780134757599", 2019);

        assertThatThrownBy(() -> bookRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("LIB-04 search matches title and author case-insensitively by substring")
    void searchMatchesCaseInsensitiveSubstrings() {
        bookRepository.saveAndFlush(new Book("Zebra Patterns In Java", "Quentin Zorro", "9781111111111", 2010));
        bookRepository.saveAndFlush(new Book("Zebra Stripes", "Ann Other", "9781111111112", 2011));
        bookRepository.saveAndFlush(new Book("Unrelated", "Quentin Zorro", "9781111111113", 2012));
        PageRequest firstPage = PageRequest.of(0, 10, Sort.by("title"));

        Page<Book> byTitle = bookRepository.search("%zebra%", "%", firstPage);
        Page<Book> byAuthor = bookRepository.search("%", "%zorro%", firstPage);
        Page<Book> byBoth = bookRepository.search("%zebra%", "%zorro%", firstPage);

        assertThat(byTitle.getContent())
                .extracting(Book::getTitle)
                .containsExactly("Zebra Patterns In Java", "Zebra Stripes");
        assertThat(byAuthor.getContent())
                .extracting(Book::getTitle)
                .containsExactly("Unrelated", "Zebra Patterns In Java");
        assertThat(byBoth.getContent()).extracting(Book::getTitle).containsExactly("Zebra Patterns In Java");
    }

    @Test
    @DisplayName("LIB-04 search treats an escaped wildcard as a literal character")
    void searchTreatsEscapedWildcardLiterally() {
        bookRepository.saveAndFlush(new Book("Qx 100% Sure", "Writer", "9782222222221", 2015));
        bookRepository.saveAndFlush(new Book("Qx 1000 Ways", "Writer", "9782222222222", 2016));

        Page<Book> result = bookRepository.search("%qx 100!%%", "%", PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Book::getTitle).containsExactly("Qx 100% Sure");
    }

    @Test
    @DisplayName("LIB-04 search pages the result and reports the total")
    void searchPagesResult() {
        for (int index = 0; index < 3; index++) {
            bookRepository.saveAndFlush(new Book("Pagewise " + index, "Pager", "978333333333" + index, 2020));
        }

        Page<Book> secondPage = bookRepository.search("%pagewise%", "%", PageRequest.of(1, 2, Sort.by("title")));

        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getContent()).extracting(Book::getTitle).containsExactly("Pagewise 2");
    }

    @Test
    @DisplayName("LIB-08 ISBN existence check can exclude the book being updated")
    void existsByIsbnExcludingSelf() {
        Book saved = bookRepository.saveAndFlush(new Book("Domain-Driven Design", "Eric Evans", "9780321125217", 2003));

        boolean takenByAnother = bookRepository.existsByIsbnAndIdNot("9780321125217", saved.getId());
        boolean taken = bookRepository.existsByIsbn("9780321125217");

        assertThat(takenByAnother).isFalse();
        assertThat(taken).isTrue();
    }

    @Test
    @DisplayName("LIB-12 marking a book borrowed succeeds once and then matches no row")
    void markBorrowedSucceedsOnlyWhileAvailable() {
        UUID bookId = bookRepository
                .saveAndFlush(new Book("Borrowable", "Lender", "9784444444441", 2001))
                .getId();

        int first = bookRepository.markBorrowedIfAvailable(bookId);
        int second = bookRepository.markBorrowedIfAvailable(bookId);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(bookRepository.findById(bookId))
                .get()
                .extracting(Book::isBorrowed)
                .isEqualTo(true);
    }

    @Test
    @DisplayName("LIB-11 marking an unknown book borrowed matches no row")
    void markBorrowedUnknownMatchesNothing() {
        int updated = bookRepository.markBorrowedIfAvailable(UUID.randomUUID());

        assertThat(updated).isZero();
    }

    @Test
    @DisplayName("LIB-15 marking a book returned only matches a borrowed book")
    void markReturnedOnlyWhileBorrowed() {
        UUID bookId = bookRepository
                .saveAndFlush(new Book("Returnable", "Lender", "9784444444442", 2002))
                .getId();

        int whileAvailable = bookRepository.markReturnedIfBorrowed(bookId);
        bookRepository.markBorrowedIfAvailable(bookId);
        int whileBorrowed = bookRepository.markReturnedIfBorrowed(bookId);

        assertThat(whileAvailable).isZero();
        assertThat(whileBorrowed).isEqualTo(1);
        assertThat(bookRepository.findById(bookId))
                .get()
                .extracting(Book::isBorrowed)
                .isEqualTo(false);
    }

    @Test
    @DisplayName("LIB-14 the open loan of a book is found and a returned loan is not")
    void findsOnlyOpenLoan() {
        Book book = bookRepository.saveAndFlush(new Book("Loaned", "Lender", "9784444444443", 2003));
        Loan returned = new Loan(book, "member-1", Instant.parse("2026-01-01T00:00:00Z"));
        returned.markReturned(Instant.parse("2026-01-02T00:00:00Z"));
        loanRepository.saveAndFlush(returned);
        Loan open = loanRepository.saveAndFlush(new Loan(book, "member-2", Instant.parse("2026-02-01T00:00:00Z")));

        Optional<Loan> found = loanRepository.findByBookIdAndReturnedAtIsNull(book.getId());

        assertThat(found).get().extracting(Loan::getId).isEqualTo(open.getId());
        assertThat(found).get().extracting(Loan::getMemberId).isEqualTo("member-2");
    }
}
