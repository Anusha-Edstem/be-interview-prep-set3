package com.interviewprep.booking.repository;

import com.interviewprep.booking.entity.Doctor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {}
