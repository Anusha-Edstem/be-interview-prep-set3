package com.interviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewprep.booking.config.BookingProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlotCalendarTest {

    private static final LocalDate DAY = LocalDate.parse("2026-10-15");

    @Test
    @DisplayName("BOOK-02 a 09:00-17:00 day has sixteen back-to-back 30-minute windows")
    void sixteenHalfHourWindows() {
        SlotCalendar calendar = new SlotCalendar(properties(ZoneOffset.UTC, "17:00"));

        List<SlotWindow> windows = calendar.windowsOn(DAY);

        assertThat(windows).hasSize(16);
        assertThat(windows.get(0))
                .isEqualTo(
                        new SlotWindow(Instant.parse("2026-10-15T09:00:00Z"), Instant.parse("2026-10-15T09:30:00Z")));
        assertThat(windows.get(15).end()).isEqualTo(Instant.parse("2026-10-15T17:00:00Z"));
        for (int i = 1; i < windows.size(); i++) {
            assertThat(windows.get(i).start()).isEqualTo(windows.get(i - 1).end());
        }
    }

    @Test
    @DisplayName("BOOK-02 the working day is interpreted in the configured zone")
    void windowsFollowConfiguredZone() {
        SlotCalendar calendar = new SlotCalendar(properties(ZoneId.of("Asia/Kolkata"), "17:00"));

        List<SlotWindow> windows = calendar.windowsOn(DAY);

        assertThat(windows.get(0).start()).isEqualTo(Instant.parse("2026-10-15T03:30:00Z"));
    }

    @Test
    @DisplayName("BOOK-02 a window that would run past the end of the day is not offered")
    void partialTrailingWindowIsDropped() {
        SlotCalendar calendar = new SlotCalendar(properties(ZoneOffset.UTC, "10:45"));

        List<SlotWindow> windows = calendar.windowsOn(DAY);

        assertThat(windows).hasSize(3);
        assertThat(windows.get(2).end()).isEqualTo(Instant.parse("2026-10-15T10:30:00Z"));
    }

    @Test
    @DisplayName("BOOK-02 configuration with the day ending before it starts is rejected")
    void invalidWorkingDayIsRejected() {
        assertThatThrownBy(() -> new BookingProperties(
                        Duration.ofMinutes(30),
                        Duration.ofMinutes(5),
                        LocalTime.of(17, 0),
                        LocalTime.of(9, 0),
                        ZoneOffset.UTC,
                        Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("day-end");
    }

    private static BookingProperties properties(ZoneId zone, String dayEnd) {
        return new BookingProperties(
                Duration.ofMinutes(30),
                Duration.ofMinutes(5),
                LocalTime.of(9, 0),
                LocalTime.parse(dayEnd),
                zone,
                Duration.ofSeconds(30));
    }
}
