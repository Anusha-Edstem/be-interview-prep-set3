package com.interviewprep.booking.dto;

import java.util.UUID;

public record DoctorResponse(UUID id, String name, String specialty) {}
