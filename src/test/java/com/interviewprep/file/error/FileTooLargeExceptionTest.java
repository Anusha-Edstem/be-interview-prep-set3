package com.interviewprep.file.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;

class FileTooLargeExceptionTest {

    @Test
    @DisplayName("FILE-03 a whole-megabyte limit is described in MB")
    void describesWholeMegabytes() {
        FileTooLargeException exception = new FileTooLargeException(6_000_000, DataSize.ofMegabytes(5));

        assertThat(exception.getMessage())
                .isEqualTo("The file is 6000000 bytes, which exceeds the maximum allowed size of 5 MB");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(exception.getType()).isEqualTo("file-too-large");
        assertThat(exception.getTitle()).isEqualTo("File too large");
    }

    @Test
    @DisplayName("FILE-03 a limit that is not a whole number of megabytes is described in bytes")
    void describesOtherLimitsInBytes() {
        FileTooLargeException exception = new FileTooLargeException(2000, DataSize.ofBytes(1500));

        assertThat(exception.getMessage())
                .isEqualTo("The file is 2000 bytes, which exceeds the maximum allowed size of 1500 bytes");
    }
}
