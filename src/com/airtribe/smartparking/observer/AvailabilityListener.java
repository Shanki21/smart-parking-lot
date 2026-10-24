package com.airtribe.smartparking.observer;

import com.airtribe.smartparking.model.AvailabilitySnapshot;

// Observer: anything that wants live availability updates (display board, mobile app feed, ...)
public interface AvailabilityListener {

    void onAvailabilityChanged(String event, AvailabilitySnapshot snapshot);
}