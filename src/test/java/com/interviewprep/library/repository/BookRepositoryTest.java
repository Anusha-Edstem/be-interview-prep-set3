package com.interviewprep.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewprep.library.entity.Book;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

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
    @DisplayName("LIB-08 ISBN existence check can exclude the book being updated")
    void existsByIsbnExcludingSelf() {
        Book saved = bookRepository.saveAndFlush(new Book("Domain-Driven Design", "Eric Evans", "9780321125217", 2003));

        boolean takenByAnother = bookRepository.existsByIsbnAndIdNot("9780321125217", saved.getId());
        boolean taken = bookRepository.existsByIsbn("9780321125217");

        assertThat(takenByAnother).isFalse();
        assertThat(taken).isTrue();
    }
}
