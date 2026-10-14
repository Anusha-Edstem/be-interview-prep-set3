package com.interviewprep.file.dto;

import java.time.Instant;
import java.util.UUID;

public record StoredFileResponse(UUID id, String originalName, String contentType, long size, Instant uploadedAt) {}
