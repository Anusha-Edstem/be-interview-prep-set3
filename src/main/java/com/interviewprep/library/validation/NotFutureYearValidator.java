package com.interviewprep.library.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Clock;
import java.time.Year;

public class NotFutureYearValidator implements ConstraintValidator<NotFutureYear, Integer> {

    private final Clock clock;

    public NotFutureYearValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(Integer year, ConstraintValidatorContext context) {
        return year == null || year <= Year.now(clock).getValue();
    }
}
