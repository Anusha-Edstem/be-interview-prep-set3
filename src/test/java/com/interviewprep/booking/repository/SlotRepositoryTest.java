package com.interviewprep.booking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewprep.booking.entity.Slot;
import com.interviewprep.booking.entity.SlotStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SlotRepositoryTest {

    private static final UUID DOCTOR_ID = UUID.fromString("6f1c2a3e-4b5d-4e6f-8a7b-0c1d2e3f4a51");
    private static final Instant NINE = Instant.parse("2026-10-15T09:00:00Z");
    private static final Duration SLOT = Duration.ofMinutes(30);

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    @DisplayName("BOOK-01 migration seeds the doctors")
    void migrationSeedsDoctors() {
        assertThat(doctorRepository.findAll()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(doctorRepository.existsById(DOCTOR_ID)).isTrue();
    }

    @Test
    @DisplayName("BOOK-02 the same doctor cannot have two slots starting at the same time")
    void uniqueDoctorAndStart() {
        slotRepository.saveAndFlush(Slot.available(DOCTOR_ID, NINE, NINE.plus(SLOT)));

        Slot duplicate = Slot.available(DOCTOR_ID, NINE, NINE.plus(SLOT));

        assertThatThrownBy(() -> slotRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("BOOK-02 bookable slots are available ones within the day, sorted by start")
    void findBookableReturnsAvailableSlotsInOrder() {
        Instant tenThirty = NINE.plus(SLOT.multipliedBy(3));
        Slot later = slotRepository.save(Slot.available(DOCTOR_ID, tenThirty, tenThirty.plus(SLOT)));
        Slot earlier = slotRepository.save(Slot.available(DOCTOR_ID, NINE, NINE.plus(SLOT)));
        Instant nextDay = NINE.plus(Duration.ofDays(1));
        slotRepository.save(Slot.available(DOCTOR_ID, nextDay, nextDay.plus(SLOT)));
        slotRepository.flush();

        List<Slot> bookable =
                slotRepository.findBookable(DOCTOR_ID, NINE, NINE.plus(Duration.ofHours(8)), NINE.minusSeconds(60));

        assertThat(bookable).extracting(Slot::getId).containsExactly(earlier.getId(), later.getId());
        assertThat(bookable).extracting(Slot::getStatus).containsOnly(SlotStatus.AVAILABLE);
    }

    @Test
    @DisplayName("BOOK-02 existing start times are read back for the requested window")
    void findStartTimesWithinWindow() {
        slotRepository.save(Slot.available(DOCTOR_ID, NINE, NINE.plus(SLOT)));
        slotRepository.flush();

        List<Instant> starts = slotRepository.findStartTimes(DOCTOR_ID, NINE, NINE.plus(SLOT));

        assertThat(starts).containsExactly(NINE);
    }
}
