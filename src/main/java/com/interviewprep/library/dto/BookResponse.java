package com.interviewprep.library.dto;

import java.time.Instant;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String title,
        String author,
        String isbn,
        int publishedYear,
        boolean available,
        Instant createdAt,
        Instant updatedAt) {}
