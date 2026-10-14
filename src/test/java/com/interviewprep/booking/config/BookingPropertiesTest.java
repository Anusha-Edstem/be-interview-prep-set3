package com.interviewprep.booking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookingPropertiesTest {

    private static final Duration SLOT = Duration.ofMinutes(30);
    private static final Duration HOLD = Duration.ofMinutes(5);
    private static final Duration SWEEP = Duration.ofSeconds(30);
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime FIVE_PM = LocalTime.of(17, 0);

    @Test
    @DisplayName("BOOK-02 valid configuration is accepted")
    void validConfigurationIsAccepted() {
        BookingProperties properties = properties(SLOT, HOLD, FIVE_PM, ZoneOffset.UTC, SWEEP);

        assertThat(properties.slotLength()).isEqualTo(SLOT);
    }

    @Test
    @DisplayName("BOOK-02 a zero slot length is rejected")
    void zeroSlotLengthIsRejected() {
        assertThatThrownBy(() -> properties(Duration.ZERO, HOLD, FIVE_PM, ZoneOffset.UTC, SWEEP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("booking.slot-length must be positive");
    }

    @Test
    @DisplayName("BOOK-12 a negative hold duration is rejected")
    void negativeHoldDurationIsRejected() {
        assertThatThrownBy(() -> properties(SLOT, Duration.ofMinutes(-5), FIVE_PM, ZoneOffset.UTC, SWEEP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("booking.hold-duration must be positive");
    }

    @Test
    @DisplayName("BOOK-12 a missing expiry sweep interval is rejected")
    void missingSweepIntervalIsRejected() {
        assertThatThrownBy(() -> properties(SLOT, HOLD, FIVE_PM, ZoneOffset.UTC, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("booking.expiry-sweep-interval must be positive");
    }

    @Test
    @DisplayName("BOOK-02 a slot longer than the working day is rejected")
    void slotLongerThanWorkingDayIsRejected() {
        assertThatThrownBy(() -> properties(SLOT, HOLD, LocalTime.of(9, 20), ZoneOffset.UTC, SWEEP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("booking.slot-length must fit between day-start and day-end");
    }

    @Test
    @DisplayName("BOOK-02 a missing zone is rejected")
    void missingZoneIsRejected() {
        assertThatThrownBy(() -> properties(SLOT, HOLD, FIVE_PM, null, SWEEP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("booking.zone is required");
    }

    private static BookingProperties properties(
            Duration slotLength, Duration holdDuration, LocalTime dayEnd, ZoneId zone, Duration sweepInterval) {
        return new BookingProperties(slotLength, holdDuration, NINE, dayEnd, zone, sweepInterval);
    }
}
