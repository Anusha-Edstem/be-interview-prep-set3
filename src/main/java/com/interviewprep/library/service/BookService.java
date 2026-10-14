package com.interviewprep.library.service;

import com.interviewprep.library.dto.BookRequest;
import com.interviewprep.library.dto.BookResponse;
import com.interviewprep.library.entity.Book;
import com.interviewprep.library.exception.DuplicateIsbnException;
import com.interviewprep.library.mapper.BookMapper;
import com.interviewprep.library.repository.BookRepository;
import com.interviewprep.shared.error.NotFoundException;
import com.interviewprep.shared.web.PageResponse;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BookService {

    private static final String BOOK = "Book";

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public BookResponse create(BookRequest request) {
        String isbn = normalizeIsbn(request.isbn());
        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateIsbnException(isbn);
        }
        Book book = new Book(request.title().strip(), request.author().strip(), isbn, request.publishedYear());
        return bookMapper.toResponse(bookRepository.saveAndFlush(book));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> list(Pageable pageable) {
        return PageResponse.of(bookRepository.findAll(pageable), bookMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BookResponse get(UUID id) {
        return bookMapper.toResponse(findBook(id));
    }

    public BookResponse update(UUID id, BookRequest request) {
        Book book = findBook(id);
        String isbn = normalizeIsbn(request.isbn());
        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new DuplicateIsbnException(isbn);
        }
        book.replaceDetails(request.title().strip(), request.author().strip(), isbn, request.publishedYear());
        return bookMapper.toResponse(bookRepository.saveAndFlush(book));
    }

    public void delete(UUID id) {
        if (!bookRepository.existsById(id)) {
            throw new NotFoundException(BOOK, id);
        }
        bookRepository.deleteById(id);
    }

    private Book findBook(UUID id) {
        return bookRepository.findById(id).orElseThrow(() -> new NotFoundException(BOOK, id));
    }

    private static String normalizeIsbn(String isbn) {
        return isbn.replace("-", "").toUpperCase(Locale.ROOT);
    }
}
