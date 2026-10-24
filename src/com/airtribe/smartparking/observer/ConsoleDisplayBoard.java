package com.airtribe.smartparking.observer;

import com.airtribe.smartparking.model.AvailabilitySnapshot;

// Simulates the electronic board at the entrance by printing every change
public class ConsoleDisplayBoard implements AvailabilityListener {

    @Override
    public void onAvailabilityChanged(String event, AvailabilitySnapshot snapshot) {
        System.out.println("  [BOARD] " + event + " | " + snapshot);
    }
}