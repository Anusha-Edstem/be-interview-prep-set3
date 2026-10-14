package com.interviewprep.booking.repository;

import com.interviewprep.booking.entity.Slot;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    @Query("""
            select s.startTime from Slot s
            where s.doctorId = :doctorId and s.startTime >= :from and s.startTime < :to
            """)
    List<Instant> findStartTimes(
            @Param("doctorId") UUID doctorId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select s from Slot s
            where s.doctorId = :doctorId
              and s.startTime >= :from and s.startTime < :to
              and (s.status = com.interviewprep.booking.entity.SlotStatus.AVAILABLE
                   or (s.status = com.interviewprep.booking.entity.SlotStatus.HELD and s.holdExpiresAt <= :now))
            order by s.startTime
            """)
    List<Slot> findBookable(
            @Param("doctorId") UUID doctorId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("now") Instant now);
}
