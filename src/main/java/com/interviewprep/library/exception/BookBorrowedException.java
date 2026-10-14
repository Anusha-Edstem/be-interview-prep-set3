package com.interviewprep.library.exception;

import com.interviewprep.shared.error.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class BookBorrowedException extends DomainException {

    public BookBorrowedException(UUID bookId) {
        super(
                HttpStatus.CONFLICT,
                "book-borrowed",
                "Book borrowed",
                "Book " + bookId + " is currently borrowed and cannot be deleted until it is returned");
    }
}
