package com.interviewprep.library.controller;

import com.interviewprep.library.dto.BookRequest;
import com.interviewprep.library.dto.BookResponse;
import com.interviewprep.library.dto.BorrowRequest;
import com.interviewprep.library.dto.LoanResponse;
import com.interviewprep.library.service.BookService;
import com.interviewprep.library.service.LoanService;
import com.interviewprep.shared.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    static final int MAX_PAGE_SIZE = 100;
    private static final Sort BY_TITLE = Sort.by("title", "id");

    private final BookService bookService;
    private final LoanService loanService;

    public BookController(BookService bookService, LoanService loanService) {
        this.bookService = bookService;
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        BookResponse created = bookService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<BookResponse> list(
            @RequestParam(required = false) @Size(max = 255) String title,
            @RequestParam(required = false) @Size(max = 255) String author,
            @PageableDefault(size = 20) Pageable pageable) {
        return bookService.list(title, author, capped(pageable));
    }

    @GetMapping("/{id}")
    public BookResponse get(@PathVariable UUID id) {
        return bookService.get(id);
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable UUID id, @Valid @RequestBody BookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        bookService.delete(id);
    }

    @PostMapping("/{id}/borrow")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse borrow(@PathVariable UUID id, @Valid @RequestBody BorrowRequest request) {
        return loanService.borrow(id, request);
    }

    @PostMapping("/{id}/return")
    public LoanResponse returnBook(@PathVariable UUID id) {
        return loanService.returnBook(id);
    }

    private static Pageable capped(Pageable requested) {
        return PageRequest.of(requested.getPageNumber(), Math.min(requested.getPageSize(), MAX_PAGE_SIZE), BY_TITLE);
    }
}
