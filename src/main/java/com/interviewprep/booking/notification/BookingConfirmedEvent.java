package com.interviewprep.booking.notification;

import java.time.Instant;
import java.util.UUID;

public record BookingConfirmedEvent(UUID slotId, UUID doctorId, Instant startTime) {}
