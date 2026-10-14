package com.interviewprep.file.error;

import com.interviewprep.shared.error.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;

public class FileTooLargeException extends DomainException {

    private static final long BYTES_PER_MEGABYTE = DataSize.ofMegabytes(1).toBytes();

    public FileTooLargeException(long size, DataSize maximum) {
        super(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "file-too-large",
                "File too large",
                "The file is " + size + " bytes, which exceeds the maximum allowed size of " + describe(maximum));
    }

    private static String describe(DataSize size) {
        long bytes = size.toBytes();
        if (bytes % BYTES_PER_MEGABYTE == 0) {
            return size.toMegabytes() + " MB";
        }
        return bytes + " bytes";
    }
}
