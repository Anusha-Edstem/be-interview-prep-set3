package com.interviewprep.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

class SlotListingIntegrationTest extends BookingIntegrationTestSupport {

    @Test
    @DisplayName("BOOK-02 first read of a day creates its 30-minute slots and later reads reuse them")
    void listingMaterialisesTheDayOnce() {
        ResponseEntity<JsonNode> first = listSlots(DOCTOR_ID, DAY);
        ResponseEntity<JsonNode> second = listSlots(DOCTOR_ID, DAY);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody()).hasSize(16);
        assertThat(first.getBody().get(0).get("startTime").asText()).isEqualTo("2026-10-15T09:00:00Z");
        assertThat(first.getBody().get(15).get("endTime").asText()).isEqualTo("2026-10-15T17:00:00Z");
        assertThat(second.getBody()).isEqualTo(first.getBody());
        assertThat(slotRepository.count()).isEqualTo(16);
    }

    @Test
    @DisplayName("BOOK-02 concurrent first reads of the same day never duplicate slots")
    void concurrentListingCreatesEachSlotOnce() throws Exception {
        int readers = 8;
        ExecutorService pool = Executors.newFixedThreadPool(readers);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<HttpStatusCode>> results = new ArrayList<>();
        for (int i = 0; i < readers; i++) {
            results.add(pool.submit(() -> {
                go.await();
                return listSlots(DOCTOR_ID, DAY).getStatusCode();
            }));
        }

        go.countDown();
        List<HttpStatusCode> statuses = new ArrayList<>();
        for (Future<HttpStatusCode> result : results) {
            statuses.add(result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(statuses).containsOnly(HttpStatus.OK);
        assertThat(slotRepository.count()).isEqualTo(16);
    }

    @Test
    @DisplayName("BOOK-04 slots of an unknown doctor are a 404 problem")
    void unknownDoctorIsNotFound() {
        ResponseEntity<JsonNode> response = listSlots(UUID.randomUUID(), DAY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("title").asText()).isEqualTo("Resource not found");
    }
}
