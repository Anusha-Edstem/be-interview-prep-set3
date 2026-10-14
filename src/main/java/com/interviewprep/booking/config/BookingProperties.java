package com.interviewprep.booking.config;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;

@ConfigurationProperties(prefix = "booking")
public record BookingProperties(
        Duration slotLength,
        Duration holdDuration,
        @DateTimeFormat(pattern = "HH:mm") LocalTime dayStart,
        @DateTimeFormat(pattern = "HH:mm") LocalTime dayEnd,
        ZoneId zone,
        Duration expirySweepInterval) {

    public BookingProperties {
        requirePositive(slotLength, "booking.slot-length");
        requirePositive(holdDuration, "booking.hold-duration");
        requirePositive(expirySweepInterval, "booking.expiry-sweep-interval");
        if (dayStart == null || dayEnd == null || !dayEnd.isAfter(dayStart)) {
            throw new IllegalArgumentException("booking.day-end must be after booking.day-start");
        }
        if (slotLength.compareTo(Duration.between(dayStart, dayEnd)) > 0) {
            throw new IllegalArgumentException("booking.slot-length must fit between day-start and day-end");
        }
        if (zone == null) {
            throw new IllegalArgumentException("booking.zone is required");
        }
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
