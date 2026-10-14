package com.interviewprep.booking.service;

import com.interviewprep.booking.entity.Slot;
import com.interviewprep.booking.repository.SlotRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SlotMaterializer {

    private final SlotRepository slotRepository;

    public SlotMaterializer(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createMissing(UUID doctorId, List<SlotWindow> windows) {
        if (windows.isEmpty()) {
            return;
        }
        Instant from = windows.get(0).start();
        Instant to = windows.get(windows.size() - 1).end();
        Set<Instant> existing = new HashSet<>(slotRepository.findStartTimes(doctorId, from, to));
        List<Slot> missing = windows.stream()
                .filter(window -> !existing.contains(window.start()))
                .map(window -> Slot.available(doctorId, window.start(), window.end()))
                .toList();
        slotRepository.saveAllAndFlush(missing);
    }
}
