package com.airtribe.smartparking.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// One floor and the spots it contains
public final class ParkingFloor {

    private final int floorNumber;
    private final List<ParkingSpot> spots;

    public ParkingFloor(int floorNumber, List<ParkingSpot> spots) {
        this.floorNumber = floorNumber;
        this.spots = Collections.unmodifiableList(new ArrayList<ParkingSpot>(spots)); // defensive copy
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public List<ParkingSpot> getSpots() {
        return spots;
    }
}