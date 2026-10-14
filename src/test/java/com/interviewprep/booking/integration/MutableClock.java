package com.interviewprep.booking.integration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

public class MutableClock extends Clock {

    private final AtomicReference<Instant> current;

    public MutableClock(Instant start) {
        this.current = new AtomicReference<>(start);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant(), zone);
    }

    @Override
    public Instant instant() {
        return current.get();
    }

    public void set(Instant instant) {
        current.set(instant);
    }

    public void advance(Duration duration) {
        current.updateAndGet(instant -> instant.plus(duration));
    }
}
