package com.interviewprep.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewprep.booking.config.BookingProperties;
import com.interviewprep.booking.dto.AvailableSlotResponse;
import com.interviewprep.booking.dto.DoctorResponse;
import com.interviewprep.booking.entity.Doctor;
import com.interviewprep.booking.entity.Slot;
import com.interviewprep.booking.mapper.BookingMapper;
import com.interviewprep.booking.repository.DoctorRepository;
import com.interviewprep.booking.repository.SlotRepository;
import com.interviewprep.shared.error.NotFoundException;
import com.interviewprep.shared.web.PageResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.parse("2026-10-15");
    private static final Instant NOW = Instant.parse("2026-10-14T08:00:00Z");
    private static final Instant DAY_START = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant DAY_END = Instant.parse("2026-10-15T17:00:00Z");

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private SlotMaterializer slotMaterializer;

    private BookingService service;

    @BeforeEach
    void setUp() {
        BookingProperties properties = new BookingProperties(
                Duration.ofMinutes(30),
                Duration.ofMinutes(5),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                ZoneOffset.UTC,
                Duration.ofSeconds(30));
        service = new BookingService(
                doctorRepository,
                slotRepository,
                new SlotCalendar(properties),
                slotMaterializer,
                new BookingMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("BOOK-01 doctors are listed by name with the page size capped")
    void listDoctorsCapsPageSize() {
        Doctor doctor = mock(Doctor.class);
        when(doctor.getId()).thenReturn(DOCTOR_ID);
        when(doctor.getName()).thenReturn("Dr. Asha Menon");
        when(doctor.getSpecialty()).thenReturn("General Practice");
        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(doctorRepository.findAll(pageCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(doctor), PageRequest.of(0, 100), 1));

        PageResponse<DoctorResponse> page = service.listDoctors(PageRequest.of(0, 500));

        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(BookingService.MAX_PAGE_SIZE);
        assertThat(pageCaptor.getValue().getSort().getOrderFor("name")).isNotNull();
        assertThat(page.content()).containsExactly(new DoctorResponse(DOCTOR_ID, "Dr. Asha Menon", "General Practice"));
    }

    @Test
    @DisplayName("BOOK-04 slots of an unknown doctor raise not found without creating slots")
    void unknownDoctorIsNotFound() {
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.listAvailableSlots(DOCTOR_ID, DAY)).isInstanceOf(NotFoundException.class);

        verify(slotMaterializer, never()).createMissing(any(), anyList());
    }

    @Test
    @DisplayName("BOOK-02 the day's windows are materialised and the bookable slots returned")
    void listMaterialisesThenReadsBookableSlots() {
        Slot slot = slot(DAY_START);
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
        when(slotRepository.findBookable(DOCTOR_ID, DAY_START, DAY_END, NOW)).thenReturn(List.of(slot));

        List<AvailableSlotResponse> slots = service.listAvailableSlots(DOCTOR_ID, DAY);

        ArgumentCaptor<List<SlotWindow>> windows = windowsCaptor();
        verify(slotMaterializer).createMissing(eq(DOCTOR_ID), windows.capture());
        assertThat(windows.getValue()).hasSize(16);
        assertThat(slots)
                .containsExactly(new AvailableSlotResponse(
                        slot.getId(), DOCTOR_ID, DAY_START, DAY_START.plus(Duration.ofMinutes(30))));
    }

    @Test
    @DisplayName("BOOK-02 a concurrent insert of the same slots is retried and then re-read")
    void concurrentMaterialisationIsRetried() {
        when(doctorRepository.existsById(DOCTOR_ID)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("uk_slot_doctor_start"))
                .doNothing()
                .when(slotMaterializer)
                .createMissing(eq(DOCTOR_ID), anyList());
        when(slotRepository.findBookable(DOCTOR_ID, DAY_START, DAY_END, NOW)).thenReturn(List.of());

        List<AvailableSlotResponse> slots = service.listAvailableSlots(DOCTOR_ID, DAY);

        verify(slotMaterializer, times(2)).createMissing(eq(DOCTOR_ID), anyList());
        assertThat(slots).isEmpty();
    }

    static Slot slot(Instant start) {
        Slot slot = Slot.available(DOCTOR_ID, start, start.plus(Duration.ofMinutes(30)));
        ReflectionTestUtils.setField(slot, "id", UUID.randomUUID());
        return slot;
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<SlotWindow>> windowsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
