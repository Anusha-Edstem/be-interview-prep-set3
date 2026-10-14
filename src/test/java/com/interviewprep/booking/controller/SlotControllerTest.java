package com.interviewprep.booking.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewprep.booking.dto.SlotResponse;
import com.interviewprep.booking.entity.SlotStatus;
import com.interviewprep.booking.service.BookingService;
import com.interviewprep.shared.error.ConflictException;
import com.interviewprep.shared.error.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(SlotController.class)
class SlotControllerTest {

    private static final UUID SLOT_ID = UUID.fromString("0d6f2a64-3a51-4f0e-9a43-2a7f5d3c9b10");
    private static final UUID DOCTOR_ID = UUID.fromString("6f1c2a3e-4b5d-4e6f-8a7b-0c1d2e3f4a51");
    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant END = Instant.parse("2026-10-15T09:30:00Z");
    private static final String PATIENT_BODY = "{\"patientId\":\"patient-1\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    @DisplayName("BOOK-05 holding a free slot returns it held with its expiry")
    void holdReturnsHeldSlot() throws Exception {
        Instant expiresAt = Instant.parse("2026-10-14T08:05:00Z");
        when(bookingService.hold(SLOT_ID, "patient-1"))
                .thenReturn(new SlotResponse(SLOT_ID, DOCTOR_ID, START, END, SlotStatus.HELD, "patient-1", expiresAt));

        perform("hold", SLOT_ID.toString(), PATIENT_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(SLOT_ID.toString()))
                .andExpect(jsonPath("$.doctorId").value(DOCTOR_ID.toString()))
                .andExpect(jsonPath("$.startTime").value("2026-10-15T09:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2026-10-15T09:30:00Z"))
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.patientId").value("patient-1"))
                .andExpect(jsonPath("$.holdExpiresAt").value("2026-10-14T08:05:00Z"));
    }

    @Test
    @DisplayName("BOOK-06 a blank patient id is a 400 validation problem")
    void blankPatientIsRejected() throws Exception {
        perform("hold", SLOT_ID.toString(), "{\"patientId\":\" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/validation-failed"))
                .andExpect(jsonPath("$.errors[0].field").value("patientId"));

        verify(bookingService, never()).hold(any(), anyString());
    }

    @Test
    @DisplayName("BOOK-06 a missing body or malformed slot id is a 400 problem")
    void malformedRequestIsRejected() throws Exception {
        perform("hold", SLOT_ID.toString(), "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));
        perform("hold", "not-a-uuid", PATIENT_BODY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));
    }

    @Test
    @DisplayName("BOOK-07 holding an unknown slot is a 404 problem")
    void holdUnknownSlotIsNotFound() throws Exception {
        when(bookingService.hold(SLOT_ID, "patient-1")).thenThrow(new NotFoundException("Slot", SLOT_ID));

        perform("hold", SLOT_ID.toString(), PATIENT_BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/not-found"));
    }

    @Test
    @DisplayName("BOOK-08 holding a taken slot is a 409 slot-unavailable problem")
    void holdTakenSlotIsConflict() throws Exception {
        when(bookingService.hold(SLOT_ID, "patient-1"))
                .thenThrow(new ConflictException("slot-unavailable", "Slot unavailable", "The slot is taken"));

        perform("hold", SLOT_ID.toString(), PATIENT_BODY)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/slot-unavailable"))
                .andExpect(jsonPath("$.title").value("Slot unavailable"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("BOOK-10 confirming an own hold returns the booked slot")
    void confirmReturnsBookedSlot() throws Exception {
        when(bookingService.confirm(SLOT_ID, "patient-1"))
                .thenReturn(new SlotResponse(SLOT_ID, DOCTOR_ID, START, END, SlotStatus.BOOKED, "patient-1", null));

        perform("confirm", SLOT_ID.toString(), PATIENT_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.patientId").value("patient-1"))
                .andExpect(jsonPath("$.holdExpiresAt").value(nullValue()));
    }

    @Test
    @DisplayName("BOOK-11 confirming without an active hold is a 409 hold-not-active problem")
    void confirmWithoutHoldIsConflict() throws Exception {
        when(bookingService.confirm(SLOT_ID, "patient-1"))
                .thenThrow(new ConflictException("hold-not-active", "Hold not active", "The hold has expired"));

        perform("confirm", SLOT_ID.toString(), PATIENT_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/hold-not-active"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("BOOK-06 confirm validates the patient id")
    void confirmValidatesBody() throws Exception {
        perform("confirm", SLOT_ID.toString(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("patientId"));
    }

    @Test
    @DisplayName("BOOK-07 confirming an unknown slot is a 404 problem")
    void confirmUnknownSlotIsNotFound() throws Exception {
        when(bookingService.confirm(SLOT_ID, "patient-1")).thenThrow(new NotFoundException("Slot", SLOT_ID));

        perform("confirm", SLOT_ID.toString(), PATIENT_BODY).andExpect(status().isNotFound());
    }

    private ResultActions perform(String action, String slotId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/slots/{slotId}/{action}", slotId, action)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
