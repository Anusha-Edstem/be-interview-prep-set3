package com.interviewprep.library.exception;

import com.interviewprep.shared.error.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class BookUnavailableException extends DomainException {

    public BookUnavailableException(UUID bookId) {
        super(
                HttpStatus.CONFLICT,
                "book-unavailable",
                "Book unavailable",
                "Book " + bookId + " is already borrowed and cannot be borrowed until it is returned");
    }
}
