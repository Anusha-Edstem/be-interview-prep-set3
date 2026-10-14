package com.interviewprep.booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HoldExpirySweeper {

    private static final Logger log = LoggerFactory.getLogger(HoldExpirySweeper.class);

    private final BookingService bookingService;

    public HoldExpirySweeper(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(
            fixedDelayString = "${booking.expiry-sweep-interval}",
            initialDelayString = "${booking.expiry-sweep-interval}")
    public int sweep() {
        int released = bookingService.releaseExpiredHolds();
        if (released > 0) {
            log.info("Released {} expired slot holds", released);
        }
        return released;
    }
}
