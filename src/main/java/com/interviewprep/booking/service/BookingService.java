package com.interviewprep.booking.service;

import com.interviewprep.booking.config.BookingProperties;
import com.interviewprep.booking.dto.AvailableSlotResponse;
import com.interviewprep.booking.dto.DoctorResponse;
import com.interviewprep.booking.dto.SlotResponse;
import com.interviewprep.booking.entity.Slot;
import com.interviewprep.booking.mapper.BookingMapper;
import com.interviewprep.booking.notification.BookingConfirmedEvent;
import com.interviewprep.booking.repository.DoctorRepository;
import com.interviewprep.booking.repository.SlotRepository;
import com.interviewprep.shared.error.ConflictException;
import com.interviewprep.shared.error.NotFoundException;
import com.interviewprep.shared.web.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    static final int MAX_PAGE_SIZE = 100;

    private final DoctorRepository doctorRepository;
    private final SlotRepository slotRepository;
    private final SlotCalendar slotCalendar;
    private final SlotMaterializer slotMaterializer;
    private final BookingMapper mapper;
    private final BookingProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public BookingService(
            DoctorRepository doctorRepository,
            SlotRepository slotRepository,
            SlotCalendar slotCalendar,
            SlotMaterializer slotMaterializer,
            BookingMapper mapper,
            BookingProperties properties,
            ApplicationEventPublisher events,
            Clock clock) {
        this.doctorRepository = doctorRepository;
        this.slotRepository = slotRepository;
        this.slotCalendar = slotCalendar;
        this.slotMaterializer = slotMaterializer;
        this.mapper = mapper;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorResponse> listDoctors(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        PageRequest page = PageRequest.of(pageable.getPageNumber(), size, Sort.by("name", "id"));
        return PageResponse.of(doctorRepository.findAll(page), mapper::toResponse);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<AvailableSlotResponse> listAvailableSlots(UUID doctorId, LocalDate date) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new NotFoundException("Doctor", doctorId);
        }
        List<SlotWindow> windows = slotCalendar.windowsOn(date);
        ensureSlotsExist(doctorId, windows);
        Instant from = windows.get(0).start();
        Instant to = windows.get(windows.size() - 1).end();
        return slotRepository.findBookable(doctorId, from, to, now()).stream()
                .map(mapper::toAvailable)
                .toList();
    }

    @Transactional
    public SlotResponse hold(UUID slotId, String patientId) {
        Instant now = now();
        int held = slotRepository.hold(slotId, patientId, now, now.plus(properties.holdDuration()));
        if (held == 0) {
            requireExists(slotId);
            throw new ConflictException(
                    "slot-unavailable", "Slot unavailable", "The slot is already held or booked by another patient");
        }
        return mapper.toResponse(load(slotId));
    }

    @Transactional
    public SlotResponse confirm(UUID slotId, String patientId) {
        int confirmed = slotRepository.confirm(slotId, patientId, now());
        if (confirmed == 0) {
            requireExists(slotId);
            throw new ConflictException(
                    "hold-not-active",
                    "Hold not active",
                    "The slot is not held by this patient or the hold has expired");
        }
        Slot booked = load(slotId);
        events.publishEvent(new BookingConfirmedEvent(booked.getId(), booked.getDoctorId(), booked.getStartTime()));
        return mapper.toResponse(booked);
    }

    @Transactional
    public SlotResponse cancel(UUID slotId, String patientId) {
        int cancelled = slotRepository.cancel(slotId, patientId);
        if (cancelled == 0) {
            requireExists(slotId);
            throw new ConflictException(
                    "booking-not-cancellable", "Booking not cancellable", "The slot is not booked by this patient");
        }
        return mapper.toResponse(load(slotId));
    }

    @Transactional
    public int releaseExpiredHolds() {
        return slotRepository.releaseExpiredHolds(now());
    }

    private void ensureSlotsExist(UUID doctorId, List<SlotWindow> windows) {
        try {
            slotMaterializer.createMissing(doctorId, windows);
        } catch (DataIntegrityViolationException concurrentlyCreated) {
            slotMaterializer.createMissing(doctorId, windows);
        }
    }

    private void requireExists(UUID slotId) {
        if (!slotRepository.existsById(slotId)) {
            throw new NotFoundException("Slot", slotId);
        }
    }

    private Slot load(UUID slotId) {
        return slotRepository.findById(slotId).orElseThrow(() -> new NotFoundException("Slot", slotId));
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
