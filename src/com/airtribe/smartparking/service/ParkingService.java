package com.airtribe.smartparking.service;

import com.airtribe.smartparking.model.AvailabilitySnapshot;
import com.airtribe.smartparking.model.Receipt;
import com.airtribe.smartparking.model.Ticket;
import com.airtribe.smartparking.model.Vehicle;
import com.airtribe.smartparking.observer.AvailabilityListener;

// The only entry point the outside world (Main, a future controller, a gate simulator) should use
public interface ParkingService {

    Ticket checkIn(Vehicle vehicle);

    Receipt checkOut(String ticketId);

    AvailabilitySnapshot getAvailability();

    void addAvailabilityListener(AvailabilityListener listener);
}