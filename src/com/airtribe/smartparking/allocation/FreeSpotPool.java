package com.airtribe.smartparking.allocation;

import com.airtribe.smartparking.model.AvailabilitySnapshot;
import com.airtribe.smartparking.model.ParkingSpot;
import com.airtribe.smartparking.model.SpotType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

// Thread-safe inventory of FREE spots, one sorted set per spot type
public class FreeSpotPool {

    private final Map<SpotType, ConcurrentSkipListSet<ParkingSpot>> freeSpots =
            new EnumMap<SpotType, ConcurrentSkipListSet<ParkingSpot>>(SpotType.class);
    private final Map<SpotType, AtomicInteger> freeCounts = new EnumMap<SpotType, AtomicInteger>(SpotType.class);

    public FreeSpotPool(Collection<ParkingSpot> allSpots) {
        for (SpotType type : SpotType.values()) {
            freeSpots.put(type, new ConcurrentSkipListSet<ParkingSpot>()); // sorted by floor, then id
            freeCounts.put(type, new AtomicInteger(0));
        }
        for (ParkingSpot spot : allSpots) {
            give(spot); // every spot starts free
        }
    }

    // pollFirst is atomic, so two threads can never receive the same spot
    public Optional<ParkingSpot> takeFirst(SpotType type) {
        ParkingSpot spot = freeSpots.get(type).pollFirst();
        if (spot == null) {
            return Optional.empty();
        }
        freeCounts.get(type).decrementAndGet();
        return Optional.of(spot);
    }

    // Returns a spot to the pool; add() is false if it was already free, so a double release is ignored
    public void give(ParkingSpot spot) {
        if (freeSpots.get(spot.getType()).add(spot)) {
            freeCounts.get(spot.getType()).incrementAndGet();
        }
    }

    public int freeCount(SpotType type) {
        return freeCounts.get(type).get();
    }

    public AvailabilitySnapshot snapshot() {
        Map<SpotType, Integer> counts = new EnumMap<SpotType, Integer>(SpotType.class);
        for (SpotType type : SpotType.values()) {
            counts.put(type, freeCount(type));
        }
        return new AvailabilitySnapshot(counts);
    }
}