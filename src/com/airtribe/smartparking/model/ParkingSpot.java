package com.airtribe.smartparking.model;

// Immutable physical spot; whether it is free is tracked by FreeSpotPool, not here
public final class ParkingSpot implements Comparable<ParkingSpot> {

    private final String id;
    private final int floorNumber;
    private final SpotType type;

    public ParkingSpot(String id, int floorNumber, SpotType type) {
        this.id = id;
        this.floorNumber = floorNumber;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public SpotType getType() {
        return type;
    }

    @Override
    public int compareTo(ParkingSpot other) {
        if (floorNumber != other.floorNumber) {
            return Integer.compare(floorNumber, other.floorNumber); // lower floor = nearer to the entrance
        }
        return id.compareTo(other.id); // then lower spot number
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParkingSpot)) {
            return false;
        }
        return id.equals(((ParkingSpot) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}