package com.airtribe.smartparking.allocation;

import com.airtribe.smartparking.model.ParkingSpot;
import com.airtribe.smartparking.model.SpotType;
import com.airtribe.smartparking.model.Vehicle;

import java.util.Optional;

// Smallest compatible spot type first, nearest (lowest floor / id) spot within that type
public class BestFitAllocationStrategy implements SpotAllocationStrategy {

    @Override
    public Optional<ParkingSpot> allocate(Vehicle vehicle, FreeSpotPool pool) {
        for (SpotType type : vehicle.getType().getCompatibleSpotTypes()) { // already ordered best fit first
            Optional<ParkingSpot> spot = pool.takeFirst(type);
            if (spot.isPresent()) {
                return spot; // first hit wins, large spots stay free for buses
            }
        }
        return Optional.empty(); // nothing compatible is free
    }
}