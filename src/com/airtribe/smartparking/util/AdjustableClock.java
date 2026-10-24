package com.airtribe.smartparking.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

// Clock we can move forward by hand, so demos and tests can "park for 3 hours" instantly
public class AdjustableClock extends Clock {

    private volatile Instant now;
    private final ZoneId zone;

    public AdjustableClock(Instant start) {
        this(start, ZoneId.of("UTC"));
    }

    private AdjustableClock(Instant start, ZoneId zone) {
        this.now = start;
        this.zone = zone;
    }

    public synchronized void advance(Duration amount) {
        now = now.plus(amount); // synchronized so concurrent advances do not get lost
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        return new AdjustableClock(now, newZone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}