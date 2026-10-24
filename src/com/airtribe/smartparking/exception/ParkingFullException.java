package com.airtribe.smartparking.exception;

// Thrown when no compatible spot is free for the vehicle
public class ParkingFullException extends ParkingException {

    public ParkingFullException(String message) {
        super(message);
    }
}