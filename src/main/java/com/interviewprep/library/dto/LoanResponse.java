package com.interviewprep.library.dto;

import java.time.Instant;
import java.util.UUID;

public record LoanResponse(UUID id, UUID bookId, String memberId, Instant borrowedAt, Instant returnedAt) {}
