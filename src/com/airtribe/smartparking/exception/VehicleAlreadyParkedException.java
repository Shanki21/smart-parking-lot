package com.airtribe.smartparking.exception;

// Thrown when a vehicle that is already inside tries to check in again
public class VehicleAlreadyParkedException extends ParkingException {

    public VehicleAlreadyParkedException(String message) {
        super(message);
    }
}