package com.airtribe.smartparking.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Vehicle size category; also owns the "which spots can I use" rule
public enum VehicleType {
    MOTORCYCLE(SpotType.SMALL, SpotType.MEDIUM, SpotType.LARGE), // tries the smallest fit first
    CAR(SpotType.MEDIUM, SpotType.LARGE),
    BUS(SpotType.LARGE); // a bus fits only in a large spot

    private final List<SpotType> compatibleSpotTypes;

    VehicleType(SpotType... spotTypes) {
        this.compatibleSpotTypes = Collections.unmodifiableList(Arrays.asList(spotTypes)); // read-only, best fit first
    }

    public List<SpotType> getCompatibleSpotTypes() {
        return compatibleSpotTypes;
    }
}