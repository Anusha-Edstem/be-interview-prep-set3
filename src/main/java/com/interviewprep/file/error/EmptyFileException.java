package com.interviewprep.file.error;

import com.interviewprep.shared.error.DomainException;
import org.springframework.http.HttpStatus;

public class EmptyFileException extends DomainException {

    public EmptyFileException() {
        super(HttpStatus.BAD_REQUEST, "empty-file", "Empty file", "The uploaded file is empty");
    }
}
