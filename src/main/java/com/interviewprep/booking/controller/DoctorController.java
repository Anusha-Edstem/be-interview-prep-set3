package com.interviewprep.booking.controller;

import com.interviewprep.booking.dto.AvailableSlotResponse;
import com.interviewprep.booking.dto.DoctorResponse;
import com.interviewprep.booking.service.BookingService;
import com.interviewprep.shared.web.PageResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final BookingService bookingService;

    public DoctorController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public PageResponse<DoctorResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return bookingService.listDoctors(pageable);
    }

    @GetMapping("/{doctorId}/slots")
    public List<AvailableSlotResponse> availableSlots(
            @PathVariable UUID doctorId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.listAvailableSlots(doctorId, date);
    }
}
