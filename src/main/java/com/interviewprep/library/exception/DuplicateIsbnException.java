package com.interviewprep.library.exception;

import com.interviewprep.shared.error.DomainException;
import org.springframework.http.HttpStatus;

public class DuplicateIsbnException extends DomainException {

    public DuplicateIsbnException(String isbn) {
        super(HttpStatus.CONFLICT, "duplicate-isbn", "Duplicate ISBN", "A book with ISBN " + isbn + " already exists");
    }
}
