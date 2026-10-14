package com.interviewprep.booking.notification;

public interface BookingNotifier {

    void bookingConfirmed(BookingConfirmedEvent event);
}
