package com.interviewprep.file.error;

import com.interviewprep.shared.error.DomainException;
import org.springframework.http.HttpStatus;

public class FileStorageException extends DomainException {

    public FileStorageException(String detail) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "file-storage-error", "File storage error", detail);
    }

    public FileStorageException(String detail, Throwable cause) {
        this(detail);
        initCause(cause);
    }
}
