package com.airtribe.smartparking.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

// Immutable proof of payment returned at check-out
public final class Receipt {

    private final String ticketId;
    private final Vehicle vehicle;
    private final String spotId;
    private final Instant entryTime;
    private final Instant exitTime;
    private final Duration duration;
    private final BigDecimal fee;

    public Receipt(String ticketId, Vehicle vehicle, String spotId,
                   Instant entryTime, Instant exitTime, Duration duration, BigDecimal fee) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.spotId = spotId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.duration = duration;
        this.fee = fee;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getSpotId() {
        return spotId;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public Instant getExitTime() {
        return exitTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public BigDecimal getFee() {
        return fee;
    }

    @Override
    public String toString() {
        long minutes = duration.toMinutes();
        return "Receipt[" + ticketId + ", " + vehicle + ", spot=" + spotId
                + ", stayed=" + (minutes / 60) + "h " + (minutes % 60) + "m, fee=Rs." + fee + "]";
    }
}