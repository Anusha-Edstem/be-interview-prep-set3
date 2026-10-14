package com.interviewprep.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BookingFlowIntegrationTest extends BookingIntegrationTestSupport {

    @Test
    @DisplayName("BOOK-10 a patient holds then confirms a slot and it leaves the available list")
    void holdThenConfirmBooksTheSlot() {
        UUID slotId = firstAvailableSlot();

        ResponseEntity<JsonNode> hold = post("hold", slotId, "patient-1");
        ResponseEntity<JsonNode> confirm = post("confirm", slotId, "patient-1");

        assertThat(hold.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(hold.getBody().get("holdExpiresAt").asText()).isEqualTo("2026-10-14T08:05:00Z");
        assertThat(confirm.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirm.getBody().get("status").asText()).isEqualTo("BOOKED");
        assertThat(listSlots(DOCTOR_ID, DAY).getBody())
                .extracting(slot -> slot.get("slotId").asText())
                .doesNotContain(slotId.toString())
                .hasSize(15);
    }

    @Test
    @DisplayName("BOOK-11 another patient cannot confirm someone else's hold")
    void confirmByAnotherPatientIsRejected() {
        UUID slotId = firstAvailableSlot();
        post("hold", slotId, "patient-1");

        ResponseEntity<JsonNode> confirm = post("confirm", slotId, "patient-2");

        assertThat(confirm.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(confirm.getBody().get("type").asText()).endsWith("/hold-not-active");
    }
}
