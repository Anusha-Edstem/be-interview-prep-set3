package com.interviewprep.booking.dto;

import java.time.Instant;
import java.util.UUID;

public record AvailableSlotResponse(UUID slotId, UUID doctorId, Instant startTime, Instant endTime) {}
