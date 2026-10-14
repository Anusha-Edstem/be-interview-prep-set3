package com.interviewprep.booking.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class LoggingBookingNotifierTest {

    @Test
    @DisplayName("BOOK-15 the notification logs slot, doctor and start time only")
    void logsBookingWithoutPatientData(CapturedOutput output) {
        UUID slotId = UUID.fromString("0d6f2a64-3a51-4f0e-9a43-2a7f5d3c9b10");
        UUID doctorId = UUID.fromString("6f1c2a3e-4b5d-4e6f-8a7b-0c1d2e3f4a51");
        BookingConfirmedEvent event =
                new BookingConfirmedEvent(slotId, doctorId, Instant.parse("2026-10-15T09:00:00Z"));

        new LoggingBookingNotifier().bookingConfirmed(event);

        assertThat(output)
                .contains("Booking confirmed: slot " + slotId + " doctor " + doctorId + " at 2026-10-15T09:00:00Z");
    }
}
