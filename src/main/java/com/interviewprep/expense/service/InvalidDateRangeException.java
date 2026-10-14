package com.interviewprep.expense.service;

import com.interviewprep.shared.error.DomainException;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends DomainException {

    public InvalidDateRangeException(LocalDate from, LocalDate to) {
        super(
                HttpStatus.BAD_REQUEST,
                "invalid-date-range",
                "Invalid date range",
                "from " + from + " must not be after to " + to);
    }
}
