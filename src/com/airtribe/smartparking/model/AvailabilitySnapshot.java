package com.airtribe.smartparking.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

// Immutable picture of how many spots of each type are free at one moment
public final class AvailabilitySnapshot {

    private final Map<SpotType, Integer> freeBySpotType;

    public AvailabilitySnapshot(Map<SpotType, Integer> freeBySpotType) {
        this.freeBySpotType = Collections.unmodifiableMap(new EnumMap<SpotType, Integer>(freeBySpotType)); // defensive copy
    }

    public int getFree(SpotType type) {
        Integer value = freeBySpotType.get(type);
        return value == null ? 0 : value;
    }

    public int getTotalFree() {
        int total = 0;
        for (int count : freeBySpotType.values()) {
            total += count;
        }
        return total;
    }

    @Override
    public String toString() {
        return "Free spots -> SMALL: " + getFree(SpotType.SMALL)
                + ", MEDIUM: " + getFree(SpotType.MEDIUM)
                + ", LARGE: " + getFree(SpotType.LARGE)
                + " (total " + getTotalFree() + ")";
    }
}