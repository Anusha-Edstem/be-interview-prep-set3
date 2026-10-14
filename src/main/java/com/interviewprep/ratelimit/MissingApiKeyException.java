package com.interviewprep.ratelimit;

import com.interviewprep.shared.error.DomainException;
import org.springframework.http.HttpStatus;

public class MissingApiKeyException extends DomainException {

    public MissingApiKeyException(String headerName) {
        super(
                HttpStatus.UNAUTHORIZED,
                "missing-api-key",
                "Missing API key",
                "The " + headerName + " header is required to call this endpoint");
    }
}
