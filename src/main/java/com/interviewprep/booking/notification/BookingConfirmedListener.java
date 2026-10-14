package com.interviewprep.booking.notification;

import com.interviewprep.booking.config.BookingAsyncConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingConfirmedListener {

    private final BookingNotifier notifier;

    public BookingConfirmedListener(BookingNotifier notifier) {
        this.notifier = notifier;
    }

    @Async(BookingAsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        notifier.bookingConfirmed(event);
    }
}
