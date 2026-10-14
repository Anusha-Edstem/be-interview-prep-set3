package com.interviewprep.shared.error;

import org.springframework.http.HttpStatus;

public class NotFoundException extends DomainException {

    public NotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "not-found", "Resource not found", resource + " " + id + " was not found");
    }
}
