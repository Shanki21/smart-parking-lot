package com.airtribe.smartparking.model;

import java.time.Instant;

// Parking transaction: which vehicle, which spot, when it came in and left
public final class Ticket {

    private final String id;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final Instant entryTime;
    private Instant exitTime; // guarded by "this"
    private TicketStatus status = TicketStatus.ACTIVE; // guarded by "this"

    public Ticket(String id, Vehicle vehicle, ParkingSpot spot, Instant entryTime) {
        this.id = id;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = entryTime;
    }

    // Atomic check-and-set: only the first caller wins, so a ticket can never be charged twice
    public synchronized boolean close(Instant exitTime) {
        if (status != TicketStatus.ACTIVE) {
            return false;
        }
        this.exitTime = exitTime;
        this.status = TicketStatus.CLOSED;
        return true;
    }

    public String getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public synchronized Instant getExitTime() {
        return exitTime;
    }

    public synchronized TicketStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Ticket[" + id + ", " + vehicle + ", spot=" + spot + ", entry=" + entryTime + ", " + getStatus() + "]";
    }
}