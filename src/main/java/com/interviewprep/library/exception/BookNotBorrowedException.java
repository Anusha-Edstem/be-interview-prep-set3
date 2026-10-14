package com.interviewprep.library.exception;

import com.interviewprep.shared.error.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class BookNotBorrowedException extends DomainException {

    public BookNotBorrowedException(UUID bookId) {
        super(
                HttpStatus.CONFLICT,
                "book-not-borrowed",
                "Book not borrowed",
                "Book " + bookId + " is not currently borrowed");
    }
}
