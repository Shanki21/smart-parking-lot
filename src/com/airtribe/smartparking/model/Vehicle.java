package com.airtribe.smartparking.model;

import java.util.Objects;

// Immutable vehicle identified by its licence plate
public final class Vehicle {

    private final String licensePlate;
    private final VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            throw new IllegalArgumentException("License plate must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Vehicle type must not be null");
        }
        this.licensePlate = licensePlate.trim().toUpperCase(); // "dl01ab1234 " and "DL01AB1234" are the same car
        this.type = type;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehicle)) {
            return false;
        }
        return licensePlate.equals(((Vehicle) o).licensePlate); // identity is the plate only
    }

    @Override
    public int hashCode() {
        return Objects.hash(licensePlate);
    }

    @Override
    public String toString() {
        return type + " " + licensePlate;
    }
}