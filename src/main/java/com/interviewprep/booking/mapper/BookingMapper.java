package com.interviewprep.booking.mapper;

import com.interviewprep.booking.dto.AvailableSlotResponse;
import com.interviewprep.booking.dto.DoctorResponse;
import com.interviewprep.booking.dto.SlotResponse;
import com.interviewprep.booking.entity.Doctor;
import com.interviewprep.booking.entity.Slot;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(doctor.getId(), doctor.getName(), doctor.getSpecialty());
    }

    public AvailableSlotResponse toAvailable(Slot slot) {
        return new AvailableSlotResponse(slot.getId(), slot.getDoctorId(), slot.getStartTime(), slot.getEndTime());
    }

    public SlotResponse toResponse(Slot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getDoctorId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus(),
                slot.getPatientId(),
                slot.getHoldExpiresAt());
    }
}
