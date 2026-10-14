package com.interviewprep.booking.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewprep.booking.repository.SlotRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(BookingIntegrationTestSupport.MutableClockConfiguration.class)
abstract class BookingIntegrationTestSupport {

    static final UUID DOCTOR_ID = UUID.fromString("6f1c2a3e-4b5d-4e6f-8a7b-0c1d2e3f4a51");
    static final LocalDate DAY = LocalDate.parse("2026-10-15");
    static final Instant START = Instant.parse("2026-10-14T08:00:00Z");

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected SlotRepository slotRepository;

    @Autowired
    protected MutableClock clock;

    @BeforeEach
    void resetState() {
        slotRepository.deleteAllInBatch();
        clock.set(START);
    }

    @AfterEach
    void cleanUp() {
        slotRepository.deleteAllInBatch();
    }

    ResponseEntity<JsonNode> listSlots(UUID doctorId, LocalDate date) {
        return rest.getForEntity("/api/v1/doctors/{doctorId}/slots?date={date}", JsonNode.class, doctorId, date);
    }

    UUID firstAvailableSlot() {
        JsonNode slots = listSlots(DOCTOR_ID, DAY).getBody();
        return UUID.fromString(slots.get(0).get("slotId").asText());
    }

    ResponseEntity<JsonNode> post(String action, UUID slotId, String patientId) {
        return rest.postForEntity(
                "/api/v1/slots/{slotId}/" + action, Map.of("patientId", patientId), JsonNode.class, slotId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(START);
        }
    }
}
