package com.interviewprep.shared.error;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {

    public ConflictException(String type, String title, String detail) {
        super(HttpStatus.CONFLICT, type, title, detail);
    }
}
