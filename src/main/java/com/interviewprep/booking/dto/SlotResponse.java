package com.interviewprep.booking.dto;

import com.interviewprep.booking.entity.SlotStatus;
import java.time.Instant;
import java.util.UUID;

public record SlotResponse(
        UUID slotId,
        UUID doctorId,
        Instant startTime,
        Instant endTime,
        SlotStatus status,
        String patientId,
        Instant holdExpiresAt) {}
