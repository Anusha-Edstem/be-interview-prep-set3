package com.interviewprep.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewprep.booking.dto.AvailableSlotResponse;
import com.interviewprep.booking.dto.DoctorResponse;
import com.interviewprep.booking.service.BookingService;
import com.interviewprep.shared.error.NotFoundException;
import com.interviewprep.shared.web.PageResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DoctorController.class)
class DoctorControllerTest {

    private static final UUID DOCTOR_ID = UUID.fromString("6f1c2a3e-4b5d-4e6f-8a7b-0c1d2e3f4a51");
    private static final LocalDate DAY = LocalDate.parse("2026-10-15");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    @DisplayName("BOOK-01 doctors are returned as a page")
    void listDoctors() throws Exception {
        PageResponse<DoctorResponse> page = new PageResponse<>(
                List.of(new DoctorResponse(DOCTOR_ID, "Dr. Asha Menon", "General Practice")), 0, 20, 1, 1);
        when(bookingService.listDoctors(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DOCTOR_ID.toString()))
                .andExpect(jsonPath("$.content[0].specialty").value("General Practice"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("BOOK-02 available slots for a doctor and day are returned")
    void listAvailableSlots() throws Exception {
        UUID slotId = UUID.randomUUID();
        when(bookingService.listAvailableSlots(DOCTOR_ID, DAY))
                .thenReturn(List.of(new AvailableSlotResponse(
                        slotId,
                        DOCTOR_ID,
                        Instant.parse("2026-10-15T09:00:00Z"),
                        Instant.parse("2026-10-15T09:30:00Z"))));

        mockMvc.perform(get("/api/v1/doctors/{doctorId}/slots", DOCTOR_ID).param("date", "2026-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slotId").value(slotId.toString()))
                .andExpect(jsonPath("$[0].startTime").value("2026-10-15T09:00:00Z"))
                .andExpect(jsonPath("$[0].endTime").value("2026-10-15T09:30:00Z"));
    }

    @Test
    @DisplayName("BOOK-03 a missing date is a 400 problem")
    void missingDateIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/{doctorId}/slots", DOCTOR_ID))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("BOOK-03 a malformed date or doctor id is a 400 problem")
    void malformedInputIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/doctors/{doctorId}/slots", DOCTOR_ID).param("date", "15-10-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));
        mockMvc.perform(get("/api/v1/doctors/{doctorId}/slots", "not-a-uuid").param("date", "2026-10-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad request"));
    }

    @Test
    @DisplayName("BOOK-04 an unknown doctor is a 404 problem")
    void unknownDoctorIsNotFound() throws Exception {
        when(bookingService.listAvailableSlots(DOCTOR_ID, DAY)).thenThrow(new NotFoundException("Doctor", DOCTOR_ID));

        mockMvc.perform(get("/api/v1/doctors/{doctorId}/slots", DOCTOR_ID).param("date", "2026-10-15"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://be-interview-prep/problems/not-found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
