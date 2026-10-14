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
    @DisplayName("BOOK-09 twenty patients holding the same slot at the same moment yield exactly one hold")
    void exactlyOnePatientWinsTheSlot() throws Exception {
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
                return new Attempt(patientId, post("hold", slotId, patientId));
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        go.countDown();
        List<Attempt> attempts = new ArrayList<>();
        for (Future<Attempt> future : futures) {
            attempts.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        List<Attempt> winners = attempts.stream().filter(Attempt::succeeded).toList();
        List<Attempt> conflicts = attempts.stream()
                .filter(attempt -> attempt.response().getStatusCode().isSameCodeAs(HttpStatus.CONFLICT))
                .toList();
        assertThat(winners).hasSize(1);
        assertThat(conflicts).hasSize(PATIENTS - 1);
        assertThat(conflicts)
                .allSatisfy(attempt -> assertThat(
                                attempt.response().getBody().get("type").asText())
                        .endsWith("/slot-unavailable"));
        Slot stored = slotRepository.findById(slotId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(SlotStatus.HELD);
        assertThat(stored.getPatientId()).isEqualTo(winners.get(0).patientId());
    }

    private record Attempt(String patientId, ResponseEntity<JsonNode> response) {

        boolean succeeded() {
            return response.getStatusCode().isSameCodeAs(HttpStatus.OK);
        }
    }
}
