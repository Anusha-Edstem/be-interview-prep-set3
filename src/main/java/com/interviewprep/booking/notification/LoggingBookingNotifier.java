package com.interviewprep.booking.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingBookingNotifier implements BookingNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingBookingNotifier.class);

    @Override
    public void bookingConfirmed(BookingConfirmedEvent event) {
        log.info("Booking confirmed: slot {} doctor {} at {}", event.slotId(), event.doctorId(), event.startTime());
    }
}
