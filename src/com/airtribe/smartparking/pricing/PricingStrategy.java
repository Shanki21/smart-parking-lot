package com.airtribe.smartparking.pricing;

import com.airtribe.smartparking.model.VehicleType;

import java.math.BigDecimal;
import java.time.Duration;

// Turns (vehicle type, time parked) into a fee; add new pricing rules by adding new implementations
public interface PricingStrategy {

    BigDecimal calculateFee(VehicleType type, Duration stay);
}