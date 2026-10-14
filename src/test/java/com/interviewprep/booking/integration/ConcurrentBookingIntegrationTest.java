package com.interviewprep.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewprep.booking.entity.Slot;
import com.interviewprep.booking.entity.SlotStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ConcurrentBookingIntegrationTest extends BookingIntegrationTestSupport {

    private static final int PATIENTS = 20;

    @Test
    @DisplayName("BOOK-09 twenty patients booking the same slot at the same moment yield exactly one booking")
    void exactlyOnePatientBooksTheSlot() throws Exception {
        UUID slotId = firstAvailableSlot();
        ExecutorService pool = Executors.newFixedThreadPool(PATIENTS);
        CountDownLatch ready = new CountDownLatch(PATIENTS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Attempt>> futures = new ArrayList<>();
        for (int i = 0; i < PATIENTS; i++) {
            String patientId = "patient-" + i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return book(slotId, patientId);
            }));
        }

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        go.countDown();
        List<Attempt> attempts = new ArrayList<>();
        for (Future<Attempt> future : futures) {
            attempts.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        List<Attempt> winners = attempts.stream().filter(Attempt::booked).toList();
        List<Attempt> rejected = attempts.stream().filter(Attempt::rejected).toList();
        assertThat(winners).hasSize(1);
        assertThat(rejected).hasSize(PATIENTS - 1);
        assertThat(rejected)
                .allSatisfy(attempt -> assertThat(
                                attempt.hold().getBody().get("type").asText())
                        .endsWith("/slot-unavailable"));
        Slot stored = slotRepository.findById(slotId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(stored.getPatientId()).isEqualTo(winners.get(0).patientId());
    }

    private Attempt book(UUID slotId, String patientId) {
        ResponseEntity<JsonNode> hold = post("hold", slotId, patientId);
        ResponseEntity<JsonNode> confirm =
                hold.getStatusCode().isSameCodeAs(HttpStatus.OK) ? post("confirm", slotId, patientId) : null;
        return new Attempt(patientId, hold, confirm);
    }

    private record Attempt(String patientId, ResponseEntity<JsonNode> hold, ResponseEntity<JsonNode> confirm) {

        boolean booked() {
            return confirm != null && confirm.getStatusCode().isSameCodeAs(HttpStatus.OK);
        }

        boolean rejected() {
            return hold.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT);
        }
    }
}
