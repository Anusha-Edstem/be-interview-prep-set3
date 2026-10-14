package com.interviewprep.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewprep.booking.entity.Slot;
import com.interviewprep.booking.entity.SlotStatus;
import com.interviewprep.booking.service.HoldExpirySweeper;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HoldExpiryIntegrationTest extends BookingIntegrationTestSupport {

    private static final Duration PAST_HOLD = Duration.ofMinutes(5).plusSeconds(1);

    @Autowired
    private HoldExpirySweeper sweeper;

    @Test
    @DisplayName("BOOK-12 an expired hold makes the slot available again and the old holder can no longer confirm")
    void expiredHoldFreesTheSlot() {
        UUID slotId = firstAvailableSlot();
        post("hold", slotId, "patient-1");
        boolean listedWhileHeld = containsSlot(listSlots(DOCTOR_ID, DAY).getBody(), slotId);

        clock.advance(PAST_HOLD);
        boolean listedAfterExpiry = containsSlot(listSlots(DOCTOR_ID, DAY).getBody(), slotId);
        ResponseEntity<JsonNode> secondHold = post("hold", slotId, "patient-2");
        ResponseEntity<JsonNode> lateConfirm = post("confirm", slotId, "patient-1");

        assertThat(listedWhileHeld).isFalse();
        assertThat(listedAfterExpiry).isTrue();
        assertThat(secondHold.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondHold.getBody().get("patientId").asText()).isEqualTo("patient-2");
        assertThat(lateConfirm.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(lateConfirm.getBody().get("type").asText()).endsWith("/hold-not-active");
    }

    @Test
    @DisplayName("BOOK-12 an expired hold cannot be confirmed even when nobody else took the slot")
    void expiredHoldCannotBeConfirmed() {
        UUID slotId = firstAvailableSlot();
        post("hold", slotId, "patient-1");

        clock.advance(PAST_HOLD);
        ResponseEntity<JsonNode> lateConfirm = post("confirm", slotId, "patient-1");

        assertThat(lateConfirm.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("BOOK-12 the scheduled sweep resets expired holds to available in the database")
    void sweepResetsExpiredHolds() {
        UUID slotId = firstAvailableSlot();
        post("hold", slotId, "patient-1");
        int releasedBeforeExpiry = sweeper.sweep();

        clock.advance(PAST_HOLD);
        int releasedAfterExpiry = sweeper.sweep();

        Slot stored = slotRepository.findById(slotId).orElseThrow();
        assertThat(releasedBeforeExpiry).isZero();
        assertThat(releasedAfterExpiry).isEqualTo(1);
        assertThat(stored.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(stored.getPatientId()).isNull();
    }

    private static boolean containsSlot(JsonNode slots, UUID slotId) {
        for (JsonNode slot : slots) {
            if (slot.get("slotId").asText().equals(slotId.toString())) {
                return true;
            }
        }
        return false;
    }
}
