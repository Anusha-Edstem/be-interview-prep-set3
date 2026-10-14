package com.interviewprep.booking.service;

import com.interviewprep.booking.config.BookingProperties;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SlotCalendar {

    private final BookingProperties properties;

    public SlotCalendar(BookingProperties properties) {
        this.properties = properties;
    }

    public List<SlotWindow> windowsOn(LocalDate date) {
        ZonedDateTime dayEnd = date.atTime(properties.dayEnd()).atZone(properties.zone());
        List<SlotWindow> windows = new ArrayList<>();
        ZonedDateTime start = date.atTime(properties.dayStart()).atZone(properties.zone());
        ZonedDateTime end = start.plus(properties.slotLength());
        while (!end.isAfter(dayEnd)) {
            windows.add(new SlotWindow(start.toInstant(), end.toInstant()));
            start = end;
            end = start.plus(properties.slotLength());
        }
        return List.copyOf(windows);
    }
}
