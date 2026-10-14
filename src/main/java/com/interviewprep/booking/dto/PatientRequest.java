package com.interviewprep.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientRequest(@NotBlank @Size(max = 64) String patientId) {}
