package com.airtribe.smartparking.exception;

// Thrown for unknown, blank or already-used tickets
public class InvalidTicketException extends ParkingException {

    public InvalidTicketException(String message) {
        super(message);
    }
}