package com.airtribe.smartparking.allocation;

import com.airtribe.smartparking.model.ParkingSpot;
import com.airtribe.smartparking.model.Vehicle;

import java.util.Optional;

// Decides which free spot a vehicle gets; swap implementations without touching the service
public interface SpotAllocationStrategy {

    // Removes and returns a spot from the pool, or empty if nothing suitable is free
    Optional<ParkingSpot> allocate(Vehicle vehicle, FreeSpotPool pool);
}