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
    private static final Duration HOLD = Duration.ofMinutes(5);
    private static final Instant NOW = Instant.parse("2026-10-14T08:00:00Z");

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    @DisplayName("BOOK-05 holding a free slot marks it held for the patient until the expiry")
    void holdFreeSlot() {
        Slot slot = freeSlot(NINE);

        int updated = slotRepository.hold(slot.getId(), "patient-1", NOW, NOW.plus(HOLD));

        Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
        assertThat(updated).isEqualTo(1);
        assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.HELD);
        assertThat(reloaded.getPatientId()).isEqualTo("patient-1");
        assertThat(reloaded.getHoldExpiresAt()).isEqualTo(NOW.plus(HOLD));
        assertThat(reloaded.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("BOOK-08 an active hold cannot be taken over by another patient")
    void activeHoldIsNotTakenOver() {
        Slot slot = freeSlot(NINE);
        slotRepository.hold(slot.getId(), "patient-1", NOW, NOW.plus(HOLD));

        int updated = slotRepository.hold(slot.getId(), "patient-2", NOW.plusSeconds(60), NOW.plus(HOLD));

        Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
        assertThat(updated).isZero();
        assertThat(reloaded.getPatientId()).isEqualTo("patient-1");
    }

    @Test
    @DisplayName("BOOK-12 an expired hold can be taken by another patient")
    void expiredHoldIsTakenOver() {
        Slot slot = freeSlot(NINE);
        slotRepository.hold(slot.getId(), "patient-1", NOW, NOW.plus(HOLD));
        Instant later = NOW.plus(HOLD).plusSeconds(1);

        int updated = slotRepository.hold(slot.getId(), "patient-2", later, later.plus(HOLD));

        Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
        assertThat(updated).isEqualTo(1);
        assertThat(reloaded.getPatientId()).isEqualTo("patient-2");
        assertThat(reloaded.getHoldExpiresAt()).isEqualTo(later.plus(HOLD));
    }

    @Test
    @DisplayName("BOOK-12 an active hold is hidden from the bookable list and an expired one is shown")
    void bookableListTreatsExpiredHoldAsFree() {
        Slot slot = freeSlot(NINE);
        slotRepository.hold(slot.getId(), "patient-1", NOW, NOW.plus(HOLD));
        Instant dayEnd = NINE.plus(Duration.ofHours(8));

        List<Slot> whileHeld = slotRepository.findBookable(DOCTOR_ID, NINE, dayEnd, NOW.plusSeconds(1));
        List<Slot> afterExpiry = slotRepository.findBookable(DOCTOR_ID, NINE, dayEnd, NOW.plus(HOLD));

        assertThat(whileHeld).isEmpty();
        assertThat(afterExpiry).extracting(Slot::getId).containsExactly(slot.getId());
    }

    @Test
    @DisplayName("BOOK-10 the holder confirming an active hold books the slot")
    void confirmActiveHold() {
        Slot slot = freeSlot(NINE);
        slotRepository.hold(slot.getId(), "patient-1", NOW, NOW.plus(HOLD));

        int updated = slotRepository.confirm(slot.getId(), "patient-1", NOW.plusSeconds(60));

        Slot reloaded = slotRepository.findById(slot.getId()).orElseThrow();
        assertThat(updated).isEqualTo(1);
        assertThat(reloaded.getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(reloaded.getPatientId()).isEqualTo("patient-1");
        assertThat(reloaded.getHoldExpiresAt()).isNull();
    }

    @Test
    @DisplayName("BOOK-11 a confirm by another patient, after expiry, or without a hold updates nothing")
    void confirmRequiresOwnActiveHold() {
        Slot free = freeSlot(NINE);
        Slot held = freeSlot(NINE.plus(SLOT));
        slotRepository.hold(held.getId(), "patient-1", NOW, NOW.plus(HOLD));

        int byOtherPatient = slotRepository.confirm(held.getId(), "patient-2", NOW.plusSeconds(60));
        int atExpiry = slotRepository.confirm(held.getId(), "patient-1", NOW.plus(HOLD));
        int withoutHold = slotRepository.confirm(free.getId(), "patient-1", NOW);

        assertThat(byOtherPatient).isZero();
        assertThat(atExpiry).isZero();
        assertThat(withoutHold).isZero();
        assertThat(slotRepository.findById(held.getId()).orElseThrow().getStatus())
                .isEqualTo(SlotStatus.HELD);
    }

    @Test
    @DisplayName("BOOK-08 a booked slot cannot be held again")
    void bookedSlotCannotBeHeld() {
        Slot slot = freeSlot(NINE);
        slotRepository.hold(slot.getId(), "patient-1", NOW, NOW.plus(HOLD));
        slotRepository.confirm(slot.getId(), "patient-1", NOW);
        Instant muchLater = NOW.plus(Duration.ofHours(1));

        int updated = slotRepository.hold(slot.getId(), "patient-2", muchLater, muchLater.plus(HOLD));

        assertThat(updated).isZero();
        assertThat(slotRepository.findById(slot.getId()).orElseThrow().getPatientId())
                .isEqualTo("patient-1");
    }

    @Test
    @DisplayName("BOOK-07 a hold on a slot that does not exist updates nothing")
    void holdUnknownSlotUpdatesNothing() {
        int updated = slotRepository.hold(UUID.randomUUID(), "patient-1", NOW, NOW.plus(HOLD));

        assertThat(updated).isZero();
    }

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

    private Slot freeSlot(Instant start) {
        return slotRepository.saveAndFlush(Slot.available(DOCTOR_ID, start, start.plus(SLOT)));
    }
}
