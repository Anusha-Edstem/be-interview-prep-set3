package com.interviewprep.library.dto;

import com.interviewprep.library.validation.NotFutureYear;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String author,

        @NotBlank
        @Size(max = 17)
        @Pattern(
                regexp = "^(?:\\d-?){9}[\\dXx]$|^(?:\\d-?){12}\\d$",
                message = "must be an ISBN-10 or ISBN-13, digits with optional hyphens")
        String isbn,

        @NotNull @Min(1) @NotFutureYear Integer publishedYear) {}
