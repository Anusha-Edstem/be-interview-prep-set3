package com.interviewprep.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.interviewprep.library.dto.BookRequest;
import com.interviewprep.library.dto.BookResponse;
import com.interviewprep.library.entity.Book;
import com.interviewprep.library.exception.DuplicateIsbnException;
import com.interviewprep.library.mapper.BookMapper;
import com.interviewprep.library.repository.BookRepository;
import com.interviewprep.shared.error.NotFoundException;
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
class BookServiceTest {

    private static final UUID BOOK_ID = UUID.fromString("7d1b6f3e-2c4a-4f0e-9a51-3f1f2b9c8e10");

    @Mock
    private BookRepository bookRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, new BookMapper());
    }

    @Test
    @DisplayName("LIB-01 create normalises the ISBN, trims text and saves the book")
    void createSavesNormalisedBook() {
        BookRequest request = new BookRequest("  Clean Code ", "Robert C. Martin", "978-0-13-235088-4", 2008);
        given(bookRepository.existsByIsbn("9780132350884")).willReturn(false);
        given(bookRepository.saveAndFlush(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.create(request);

        ArgumentCaptor<Book> saved = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getIsbn()).isEqualTo("9780132350884");
        assertThat(saved.getValue().getTitle()).isEqualTo("Clean Code");
        assertThat(response.available()).isTrue();
        assertThat(response.publishedYear()).isEqualTo(2008);
    }

    @Test
    @DisplayName("LIB-03 create rejects an ISBN that already exists")
    void createRejectsDuplicateIsbn() {
        BookRequest request = new BookRequest("Clean Code", "Robert C. Martin", "9780132350884", 2008);
        given(bookRepository.existsByIsbn("9780132350884")).willReturn(true);

        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(DuplicateIsbnException.class)
                .hasMessageContaining("9780132350884");
        verify(bookRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("LIB-06 get raises not found for an unknown id")
    void getUnknownRaisesNotFound() {
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.get(BOOK_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("LIB-07 update replaces the details of an existing book")
    void updateReplacesDetails() {
        Book existing = new Book("Old", "Old Author", "9780132350884", 2000);
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(existing));
        given(bookRepository.existsByIsbnAndIdNot("0306406152", BOOK_ID)).willReturn(false);
        given(bookRepository.saveAndFlush(existing)).willReturn(existing);

        BookResponse response =
                bookService.update(BOOK_ID, new BookRequest("New", "New Author", "0-306-40615-2", 2001));

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.author()).isEqualTo("New Author");
        assertThat(response.isbn()).isEqualTo("0306406152");
        assertThat(response.publishedYear()).isEqualTo(2001);
    }

    @Test
    @DisplayName("LIB-08 update rejects an ISBN that belongs to another book")
    void updateRejectsIsbnOfAnotherBook() {
        Book existing = new Book("Old", "Old Author", "9780132350884", 2000);
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.of(existing));
        given(bookRepository.existsByIsbnAndIdNot("0306406152", BOOK_ID)).willReturn(true);

        assertThatThrownBy(() -> bookService.update(BOOK_ID, new BookRequest("New", "New", "0306406152", 2001)))
                .isInstanceOf(DuplicateIsbnException.class);
        assertThat(existing.getIsbn()).isEqualTo("9780132350884");
    }

    @Test
    @DisplayName("LIB-07 update raises not found for an unknown id")
    void updateUnknownRaisesNotFound() {
        given(bookRepository.findById(BOOK_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(BOOK_ID, new BookRequest("T", "A", "0306406152", 2001)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("LIB-09 delete removes an existing book")
    void deleteRemovesBook() {
        given(bookRepository.existsById(BOOK_ID)).willReturn(true);

        bookService.delete(BOOK_ID);

        verify(bookRepository).deleteById(BOOK_ID);
    }

    @Test
    @DisplayName("LIB-09 delete raises not found for an unknown id")
    void deleteUnknownRaisesNotFound() {
        given(bookRepository.existsById(BOOK_ID)).willReturn(false);

        assertThatThrownBy(() -> bookService.delete(BOOK_ID)).isInstanceOf(NotFoundException.class);
    }
}
