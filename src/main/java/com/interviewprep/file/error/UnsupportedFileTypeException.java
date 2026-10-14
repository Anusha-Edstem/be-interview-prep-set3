package com.interviewprep.file.error;

import com.interviewprep.shared.error.DomainException;
import org.springframework.http.HttpStatus;

public class UnsupportedFileTypeException extends DomainException {

    public UnsupportedFileTypeException(String detail) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-file-type", "Unsupported file type", detail);
    }
}
