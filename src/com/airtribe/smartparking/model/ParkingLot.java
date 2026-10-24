package com.airtribe.smartparking.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// The physical layout: a name and its floors (pure data, no business rules)
public final class ParkingLot {

    private final String name;
    private final List<ParkingFloor> floors;

    private ParkingLot(String name, List<ParkingFloor> floors) {
        this.name = name;
        this.floors = Collections.unmodifiableList(floors);
    }

    public String getName() {
        return name;
    }

    public List<ParkingFloor> getFloors() {
        return floors;
    }

    public List<ParkingSpot> getAllSpots() {
        List<ParkingSpot> all = new ArrayList<ParkingSpot>();
        for (ParkingFloor floor : floors) {
            all.addAll(floor.getSpots());
        }
        return all;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    // Builder keeps construction (and spot-id generation) out of the model itself
    public static final class Builder {

        private final String name;
        private final List<ParkingFloor> floors = new ArrayList<ParkingFloor>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder addFloor(int smallSpots, int mediumSpots, int largeSpots) {
            int floorNumber = floors.size() + 1; // floors are numbered 1, 2, 3...
            List<ParkingSpot> spots = new ArrayList<ParkingSpot>();
            addSpots(spots, floorNumber, SpotType.SMALL, smallSpots);
            addSpots(spots, floorNumber, SpotType.MEDIUM, mediumSpots);
            addSpots(spots, floorNumber, SpotType.LARGE, largeSpots);
            floors.add(new ParkingFloor(floorNumber, spots));
            return this; // fluent style
        }

        private void addSpots(List<ParkingSpot> spots, int floorNumber, SpotType type, int count) {
            for (int i = 1; i <= count; i++) {
                String id = String.format("F%d-%c%03d", floorNumber, type.getCode(), i); // e.g. F1-M002
                spots.add(new ParkingSpot(id, floorNumber, type));
            }
        }

        public ParkingLot build() {
            if (floors.isEmpty()) {
                throw new IllegalStateException("A parking lot needs at least one floor");
            }
            return new ParkingLot(name, new ArrayList<ParkingFloor>(floors));
        }
    }
}