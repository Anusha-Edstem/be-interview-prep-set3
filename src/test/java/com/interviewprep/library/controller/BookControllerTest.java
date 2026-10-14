package com.interviewprep.library.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewprep.library.dto.BookRequest;
import com.interviewprep.library.dto.BookResponse;
import com.interviewprep.library.dto.BorrowRequest;
import com.interviewprep.library.dto.LoanResponse;
import com.interviewprep.library.exception.BookNotBorrowedException;
import com.interviewprep.library.exception.BookUnavailableException;
import com.interviewprep.library.exception.DuplicateIsbnException;
import com.interviewprep.library.service.BookService;
import com.interviewprep.library.service.LoanService;
import com.interviewprep.shared.error.NotFoundException;
import com.interviewprep.shared.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
@Import(BookControllerTest.FixedClockConfig.class)
class BookControllerTest {

    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");
    private static final UUID BOOK_ID = UUID.fromString("7d1b6f3e-2c4a-4f0e-9a51-3f1f2b9c8e10");
    private static final UUID LOAN_ID = UUID.fromString("0b9e5f0a-6d3c-4b8e-8a7f-1c2d3e4f5a6b");

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private LoanService loanService;

    @Test
    @DisplayName("LIB-01 creating a valid book returns 201 with a Location header and the book")
    void createReturnsCreated() throws Exception {
        given(bookService.create(any(BookRequest.class))).willReturn(sampleBook());

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/books/" + BOOK_ID))
                .andExpect(jsonPath("$.id").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("LIB-02 creating a book with blank fields, a bad ISBN and a future year returns 400 with field errors")
    void createRejectsInvalidBody() throws Exception {
        String invalid = """
                {"title": " ", "author": "", "isbn": "12-34", "publishedYear": 2027}
                """;

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/validation-failed"))
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'author')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'isbn')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'publishedYear')].message")
                        .value("must not be in the future"));
        verify(bookService, never()).create(any());
    }

    @Test
    @DisplayName("LIB-02 a book published in the current year is accepted")
    void createAcceptsCurrentYear() throws Exception {
        given(bookService.create(any(BookRequest.class))).willReturn(sampleBook());
        String currentYear = """
                {"title": "New", "author": "Author", "isbn": "0-306-40615-2", "publishedYear": 2026}
                """;

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(currentYear))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("LIB-03 creating a book with an existing ISBN returns 409 duplicate-isbn")
    void createRejectsDuplicateIsbn() throws Exception {
        given(bookService.create(any(BookRequest.class))).willThrow(new DuplicateIsbnException("9780132350884"));

        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/duplicate-isbn"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("LIB-04 listing books returns a page and caps the page size at 100")
    void listCapsPageSize() throws Exception {
        given(bookService.list(isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageResponse<>(List.of(sampleBook()), 0, 100, 1, 1));

        mockMvc.perform(get("/api/v1/books").param("page", "0").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).list(isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("LIB-04 listing books passes the title and author search terms to the service")
    void listPassesSearchTerms() throws Exception {
        given(bookService.list(eq("clean"), eq("martin"), any(Pageable.class)))
                .willReturn(new PageResponse<>(List.of(sampleBook()), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/books").param("title", "clean").param("author", "martin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("LIB-04 a search term longer than 255 characters returns 400")
    void listRejectsOverlongSearchTerm() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("title", "x".repeat(256)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
        verify(bookService, never()).list(any(), any(), any());
    }

    @Test
    @DisplayName("LIB-05 getting an existing book returns 200 with the book")
    void getReturnsBook() throws Exception {
        given(bookService.get(BOOK_ID)).willReturn(sampleBook());

        mockMvc.perform(get("/api/v1/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Robert C. Martin"));
    }

    @Test
    @DisplayName("LIB-06 getting an unknown book returns 404 problem detail")
    void getUnknownReturnsNotFound() throws Exception {
        given(bookService.get(BOOK_ID)).willThrow(new NotFoundException("Book", BOOK_ID));

        mockMvc.perform(get("/api/v1/books/{id}", BOOK_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/not-found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("LIB-06 a malformed book id returns 400")
    void malformedIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/books/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/bad-request"));
    }

    @Test
    @DisplayName("LIB-07 updating a book returns 200 with the updated book")
    void updateReturnsBook() throws Exception {
        given(bookService.update(eq(BOOK_ID), any(BookRequest.class))).willReturn(sampleBook());

        mockMvc.perform(put("/api/v1/books/{id}", BOOK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBookJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BOOK_ID.toString()));
    }

    @Test
    @DisplayName("LIB-07 updating a book with an invalid body returns 400")
    void updateRejectsInvalidBody() throws Exception {
        mockMvc.perform(put("/api/v1/books/{id}", BOOK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    @DisplayName("LIB-09 deleting a book returns 204")
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/books/{id}", BOOK_ID)).andExpect(status().isNoContent());

        verify(bookService).delete(BOOK_ID);
    }

    @Test
    @DisplayName("LIB-09 deleting an unknown book returns 404")
    void deleteUnknownReturnsNotFound() throws Exception {
        willThrow(new NotFoundException("Book", BOOK_ID)).given(bookService).delete(BOOK_ID);

        mockMvc.perform(delete("/api/v1/books/{id}", BOOK_ID)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("LIB-11 borrowing an available book returns 201 with the loan")
    void borrowReturnsCreatedLoan() throws Exception {
        given(loanService.borrow(BOOK_ID, new BorrowRequest("member-42"))).willReturn(openLoan());

        mockMvc.perform(post("/api/v1/books/{id}/borrow", BOOK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": "member-42"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(LOAN_ID.toString()))
                .andExpect(jsonPath("$.bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.memberId").value("member-42"))
                .andExpect(jsonPath("$.borrowedAt").value("2026-06-15T10:00:00Z"))
                .andExpect(jsonPath("$.returnedAt").doesNotExist());
    }

    @Test
    @DisplayName("LIB-11 borrowing without a member id returns 400 with a field error")
    void borrowRejectsBlankMember() throws Exception {
        mockMvc.perform(post("/api/v1/books/{id}/borrow", BOOK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": "  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("memberId"));
        verify(loanService, never()).borrow(any(), any());
    }

    @Test
    @DisplayName("LIB-11 borrowing an unknown book returns 404")
    void borrowUnknownReturnsNotFound() throws Exception {
        given(loanService.borrow(eq(BOOK_ID), any(BorrowRequest.class)))
                .willThrow(new NotFoundException("Book", BOOK_ID));

        mockMvc.perform(post("/api/v1/books/{id}/borrow", BOOK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": "member-42"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("LIB-12 borrowing a book that is already borrowed returns 409 book-unavailable problem detail")
    void borrowUnavailableReturnsConflictProblem() throws Exception {
        given(loanService.borrow(eq(BOOK_ID), any(BorrowRequest.class)))
                .willThrow(new BookUnavailableException(BOOK_ID));

        mockMvc.perform(post("/api/v1/books/{id}/borrow", BOOK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": "member-7"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/book-unavailable"))
                .andExpect(jsonPath("$.title").value("Book unavailable"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail")
                        .value("Book " + BOOK_ID + " is already borrowed and cannot be borrowed until it is returned"))
                .andExpect(jsonPath("$.instance").value("/api/v1/books/" + BOOK_ID + "/borrow"));
    }

    @Test
    @DisplayName("LIB-14 returning a borrowed book returns 200 with the closed loan")
    void returnReturnsClosedLoan() throws Exception {
        LoanResponse closed = new LoanResponse(LOAN_ID, BOOK_ID, "member-42", NOW, NOW.plusSeconds(3600));
        given(loanService.returnBook(BOOK_ID)).willReturn(closed);

        mockMvc.perform(post("/api/v1/books/{id}/return", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedAt").value("2026-06-15T11:00:00Z"));
    }

    @Test
    @DisplayName("LIB-15 returning a book that is not borrowed returns 409 book-not-borrowed")
    void returnNotBorrowedReturnsConflict() throws Exception {
        given(loanService.returnBook(BOOK_ID)).willThrow(new BookNotBorrowedException(BOOK_ID));

        mockMvc.perform(post("/api/v1/books/{id}/return", BOOK_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/book-not-borrowed"));
    }

    private static LoanResponse openLoan() {
        return new LoanResponse(LOAN_ID, BOOK_ID, "member-42", NOW, null);
    }

    private static String validBookJson() {
        return """
                {"title": "Clean Code", "author": "Robert C. Martin", "isbn": "978-0-13-235088-4", "publishedYear": 2008}
                """;
    }

    private static BookResponse sampleBook() {
        return new BookResponse(BOOK_ID, "Clean Code", "Robert C. Martin", "9780132350884", 2008, true, NOW, NOW);
    }
}
