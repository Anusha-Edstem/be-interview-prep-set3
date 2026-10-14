package com.interviewprep.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BorrowRequest(@NotBlank @Size(max = 100) String memberId) {}
