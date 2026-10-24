package com.airtribe.smartparking.exception;

// Base type so callers can catch every domain error with one catch block
public class ParkingException extends RuntimeException {

    public ParkingException(String message) {
        super(message); // pass the message up so getMessage() works
    }
}