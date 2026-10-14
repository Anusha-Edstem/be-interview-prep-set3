package com.interviewprep.booking.controller;

import com.interviewprep.booking.dto.PatientRequest;
import com.interviewprep.booking.dto.SlotResponse;
import com.interviewprep.booking.service.BookingService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/slots")
public class SlotController {

    private final BookingService bookingService;

    public SlotController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/{slotId}/hold")
    public SlotResponse hold(@PathVariable UUID slotId, @Valid @RequestBody PatientRequest request) {
        return bookingService.hold(slotId, request.patientId());
    }

    @PostMapping("/{slotId}/confirm")
    public SlotResponse confirm(@PathVariable UUID slotId, @Valid @RequestBody PatientRequest request) {
        return bookingService.confirm(slotId, request.patientId());
    }
}
